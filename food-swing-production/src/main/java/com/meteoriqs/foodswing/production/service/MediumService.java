package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.entity.MediumMaster;
import com.meteoriqs.foodswing.data.model.PaginationInfo;
import com.meteoriqs.foodswing.data.model.RecipeSearchResponse;
import com.meteoriqs.foodswing.data.model.Status;
import com.meteoriqs.foodswing.data.repository.MediumMasterRepository;
import net.spy.memcached.MemcachedClient;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class MediumService extends BaseService {

    private final MediumMasterRepository mediumMasterRepository;

    private final MemcachedClient memcachedClient;

    public MediumService(MediumMasterRepository mediumMasterRepository,
                         MemcachedClient memcachedClient) {
        this.mediumMasterRepository = mediumMasterRepository;
        this.memcachedClient = memcachedClient;
    }

    public Mono<CustomPaginateResponse<List<MediumMaster>>> getAllMedium(Pageable pageable) {

        return mediumMasterRepository.count()
                .flatMap(totalCount -> mediumMasterRepository.findAllWithPagination(pageable)
                        .collectList()
                        .map(paginatedMedium -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<MediumMaster>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedMedium);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All Medium Error Occurred")));
    }

    public Mono<CustomResponse<MediumMaster>> getMediumById(int mediumId) {
        return mediumMasterRepository.findById(mediumId)
                .map(medium -> new CustomResponse<>(new Status(200, "Success"), medium))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }


    public Mono<CustomResponse<Object>> createMedium(MediumMaster createMediumRequest) {
        return mediumMasterRepository.findByMediumName(createMediumRequest.getMediumName())
                .flatMap(existingMedium -> Mono.just(new CustomResponse<>(new Status(403, "Medium name already exists"), null)))
                .switchIfEmpty(Mono.defer(() -> {
                    MediumMaster newMedium = new MediumMaster();
                    newMedium.setMediumName(createMediumRequest.getMediumName());
                    newMedium.setActive(true);
                    newMedium.setCreatedBy(createMediumRequest.getCreatedBy());
                    newMedium.setCreatedTime(Instant.now());
                    return mediumMasterRepository.save(newMedium)
                            .flatMap(savedMedium -> {
                                memcachedClient.set("mediumNames-"+savedMedium.getMediumId(),0,savedMedium.getMediumName());

                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("mediumEntityList");
                                processData.add(new RecipeSearchResponse(savedMedium.getMediumId(),savedMedium.getMediumName(),0,null,null));
                                memcachedClient.set("mediumEntityList",0,processData);

                                return Mono.just(new CustomResponse<>(new Status(201, "Medium created successfully"), savedMedium));
                            });
                }));
    }


    public Mono<CustomResponse<MediumMaster>> updateMedium(int mediumId, MediumMaster updateMediumRequest) {
        return mediumMasterRepository.findById(mediumId)
                .flatMap(existingMedium -> {
                    existingMedium.setMediumName(updateMediumRequest.getMediumName());
                    existingMedium.setUpdatedBy(updateMediumRequest.getUpdatedBy());
                    existingMedium.setUpdatedTime(Instant.now());

                    return mediumMasterRepository.save(existingMedium)
                            .doOnNext(savedMedium -> {
                                memcachedClient.set("mediumNames-" + savedMedium.getMediumId(), 0, savedMedium.getMediumName());
                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("mediumEntityList");
                                processData.add(new RecipeSearchResponse(savedMedium.getMediumId(),savedMedium.getMediumName(),0,null,null));
                                memcachedClient.set("mediumEntityList",0,processData);
                            });
                })
                .map(medium -> new CustomResponse<>(new Status(200, "Medium updated successfully"), medium))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }

    public Mono<MediumMaster> deleteMedium(int mediumId) {
        return mediumMasterRepository.findById(mediumId)
                .flatMap(medium -> mediumMasterRepository.deleteById(mediumId).thenReturn(medium))
                .doOnNext(deletedMedium -> memcachedClient.delete("mediumNames-"+mediumId))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Medium not found..!!"))));
    }

    private CustomResponse<MediumMaster> getNotFoundResponse() {
        return new CustomResponse<>(new Status(404, "Medium not found"), null);
    }
}
