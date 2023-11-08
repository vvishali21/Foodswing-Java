package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.entity.CompanyDefaultCostConfig;
import com.meteoriqs.foodswing.data.entity.CompanyMealCostMapping;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.data.repository.*;
import net.spy.memcached.MemcachedClient;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompanyService extends BaseService {

    private final CompanyRepository companyRepository;
    private final GrammageRepository grammageRepository;
    private final MealItemMappingRepository mealItemMappingRepository;
    private final CompanyCostConfigRepository companyCostConfigRepository;
    private final CompanyCostMappingRepository companyCostMappingRepository;
    private final MemcachedClient memcachedClient;

    public CompanyService(CompanyRepository companyRepository, GrammageRepository grammageRepository,
                          MealItemMappingRepository mealItemMappingRepository,
                          CompanyCostConfigRepository companyCostConfigRepository,
                          CompanyCostMappingRepository companyCostMappingRepository,
                          MemcachedClient memcachedClient) {
        this.companyRepository = companyRepository;
        this.grammageRepository = grammageRepository;
        this.mealItemMappingRepository = mealItemMappingRepository;
        this.companyCostConfigRepository = companyCostConfigRepository;
        this.companyCostMappingRepository = companyCostMappingRepository;
        this.memcachedClient = memcachedClient;
    }

    public Mono<CustomPaginateResponse<List<Company>>> getAllCompany(Pageable pageable) {

        return companyRepository.count()
                .flatMap(totalCount -> companyRepository.findAllWithPagination(pageable)
                        .collectList()
                        .map(paginatedCompany -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<Company>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedCompany);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All Company Error Occurred")));
    }

    public Mono<CustomResponse<Company>> getCompanyById(int companyId) {
        return companyRepository.findById(companyId)
                .flatMap(company -> {
                    // Fetch costList from company_default_cost_config
                    return companyCostConfigRepository.findByCompanyId(companyId)
                            .collectList()
                            .map(costConfigs -> {
                                List<RecipeSearchResponse> costList = costConfigs.stream()
                                        .map(costConfig -> new RecipeSearchResponse(
                                                costConfig.getMealId(),
                                                (String) memcachedClient.get("mealNames-" + costConfig.getMealId()),
                                                0, null,
                                                costConfig.getDefaultCost()
                                        )).collect(Collectors.toList());
                                company.setCostList(costList);
                                return new CustomResponse<>(new Status(200, "Success"), company);
                            });
                })
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }



    public Mono<CustomResponse<Object>> createCompany(Company createCompanyRequest) {
        return companyRepository.findByCompanyName(createCompanyRequest.getCompanyName())
                .flatMap(existingCompany -> Mono.just(new CustomResponse<>(new Status(403, "Company name already exists"), null)))
                .switchIfEmpty(Mono.defer(() -> {
                    Company newCompany = new Company();
                    newCompany.setCompanyName(createCompanyRequest.getCompanyName());
                    newCompany.setPrimaryContactNumber(createCompanyRequest.getPrimaryContactNumber());
                    newCompany.setSecondaryContactNumber(createCompanyRequest.getSecondaryContactNumber());
                    newCompany.setEmail(createCompanyRequest.getEmail());
                    newCompany.setActive(true);
                    newCompany.setCreatedBy(createCompanyRequest.getCreatedBy());
                    newCompany.setCreatedTime(Instant.now());

                    return companyRepository.save(newCompany)
                            .flatMap(savedCompany -> {
                                memcachedClient.set("companyNames-" + savedCompany.getCompanyId(), 0, savedCompany.getCompanyName());

                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("companyEntityList");
                                processData.add(new RecipeSearchResponse(savedCompany.getCompanyId(),savedCompany.getCompanyName(),0,null,null));
                                memcachedClient.set("companyEntityList",0,processData);


                                List<CompanyDefaultCostConfig> defaultCostConfigs = createCompanyRequest.getCostList().stream()
                                        .map(cost -> {
                                            CompanyDefaultCostConfig config = new CompanyDefaultCostConfig();
                                            config.setId(0);
                                            config.setCompanyId(savedCompany.getCompanyId());
                                            config.setMealId(cost.getId());
                                            config.setDefaultCost(cost.getCost());
                                            config.setCreatedBy(createCompanyRequest.getCreatedBy());
                                            config.setCreatedTime(Instant.now());
                                            config.setActive(true);
                                            return config;
                                        })
                                        .collect(Collectors.toList());

                                return companyCostConfigRepository.saveAll(defaultCostConfigs)
                                        .collectList()
                                        .flatMap(savedDefaultCostConfigs -> {
                                            // Save CompanyMealCostMapping entries
                                            List<CompanyMealCostMapping> mealCostMappings = new ArrayList<>();
                                            for (RecipeSearchResponse meal : createCompanyRequest.getCostList()) {
                                                for (int day = 0; day <= 6; day++) {
                                                    CompanyMealCostMapping mealCostMapping = new CompanyMealCostMapping();
                                                    mealCostMapping.setCompanyId(savedCompany.getCompanyId());
                                                    mealCostMapping.setMealId(meal.getId());
                                                    mealCostMapping.setDay(day);
                                                    mealCostMapping.setCost(meal.getCost());
                                                    mealCostMapping.setCreatedBy(createCompanyRequest.getCreatedBy());
                                                    mealCostMapping.setCreatedTime(Instant.now());
                                                    mealCostMapping.setActive(true);
                                                    mealCostMappings.add(mealCostMapping);
                                                }
                                            }

                                            return companyCostMappingRepository.saveAll(mealCostMappings)
                                                    .then(createGrammageEntries(savedCompany))
                                                    .map(savedMealCostMapping->new CustomResponse<>(new Status(201, "Company created successfully"), savedCompany));
                                        });
                            });
                }));
    }


    private Mono<List<Grammage>> createGrammageEntries(Company company) {
        return mealItemMappingRepository.findAll()
                .flatMap(mealItemMapping -> Flux.range(0, 7)
                        .flatMap(dayOfWeek -> {
                            Grammage grammage = new Grammage();
                            grammage.setCompanyId(company.getCompanyId());
                            grammage.setMealId(mealItemMapping.getMealId());
                            grammage.setItemId(mealItemMapping.getItemId());
                            grammage.setGram((BigDecimal) memcachedClient.get("itemGramMapper-" + mealItemMapping.getItemId()));
                            grammage.setDay(dayOfWeek);

                            return grammageRepository.save(grammage);
                        }))
                .collectList();
    }


    public Mono<CustomPaginateResponse<List<CompanyMealCostMapping>>> getAllCompanyCostMapping(Pageable pageable) {

        return companyCostMappingRepository.count()
                .flatMap(totalCount -> companyCostMappingRepository.findAllWithPagination(pageable)
                        .collectList()
                        .map(paginatedData -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<CompanyMealCostMapping>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedData);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All Company Cost Mapping Error Occurred")));
    }

    public Mono<CustomPaginateResponse<List<Grammage>>> getAllGram(Pageable pageable) {

        return grammageRepository.count()
                .flatMap(totalCount -> grammageRepository.findAllWithPagination(pageable)
                        .collectList()
                        .map(paginatedGram -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<Grammage>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedGram);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All Grammage Error Occurred")));
    }

    public Mono<CustomResponse<List<CompanyMealCostMapping>>> updateCompanyCost(Flux<CompanyMealCostMapping> updateCompanyRequest) {
        return updateCompanyRequest
                .flatMap(updateRequest -> companyCostMappingRepository.findById(updateRequest.getId())
                        .flatMap(existingData -> {
                            if (existingData != null) {
                                existingData.setCompanyId(updateRequest.getCompanyId());
                                existingData.setMealId(updateRequest.getMealId());
                                existingData.setDay(updateRequest.getDay());
                                existingData.setCost(updateRequest.getCost());
                                existingData.setCreatedBy(updateRequest.getCreatedBy());
                                existingData.setCreatedTime(updateRequest.getCreatedTime());
                                existingData.setUpdatedBy(updateRequest.getUpdatedBy());
                                existingData.setUpdatedTime(Instant.now());
                                existingData.setActive(true);

                                return companyCostMappingRepository.save(existingData);
                            } else {
                                return Mono.empty();
                            }
                        })
                )
                .collectList()
                .flatMap(updatedCost -> {
                    if (!updatedCost.isEmpty()) {
                        return Mono.just(new CustomResponse<>(new Status(200, "Cost updated successfully"), updatedCost));
                    } else {
                        return Mono.just(new CustomResponse<>(new Status(404, "Data not found"), null));
                    }
                });
    }


    public Mono<CustomResponse<Company>> updateCompany(int companyId, Company updateCompanyRequest) {
        return companyRepository.findById(companyId)
                .flatMap(existingCompany -> {
                    existingCompany.setCompanyName(updateCompanyRequest.getCompanyName());
                    existingCompany.setPrimaryContactNumber(updateCompanyRequest.getPrimaryContactNumber());
                    existingCompany.setSecondaryContactNumber(updateCompanyRequest.getSecondaryContactNumber());
                    existingCompany.setEmail(updateCompanyRequest.getEmail());
                    existingCompany.setUpdatedBy(updateCompanyRequest.getUpdatedBy());
                    existingCompany.setUpdatedTime(Instant.now());

                    return companyRepository.save(existingCompany)
                            .doOnNext(savedCompany -> {
                                memcachedClient.set("companyNames-" + savedCompany.getCompanyId(), 0, savedCompany.getCompanyName());
                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("companyEntityList");
                                processData.add(new RecipeSearchResponse(savedCompany.getCompanyId(), savedCompany.getCompanyName(), 0, null, null));
                                memcachedClient.set("companyEntityList", 0, processData);
                            });
                })
                .map(company -> new CustomResponse<>(new Status(200, "Success"), company))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }

    public Mono<Company> deleteCompany(int companyId) {
        return companyRepository.findById(companyId)
                .flatMap(company -> companyRepository.deleteById(companyId).thenReturn(company))
                .doOnNext(deletedCompany -> memcachedClient.delete("companyNames-" + companyId))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found..!!"))));
    }

    private CustomResponse<Company> getNotFoundResponse() {
        return new CustomResponse<>(new Status(404, "Company not found"), null);
    }

}
