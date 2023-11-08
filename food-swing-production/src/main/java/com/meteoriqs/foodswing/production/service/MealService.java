package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.data.repository.MealCategoryMappingRepository;
import com.meteoriqs.foodswing.data.repository.MealRepository;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class MealService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(MealService.class);
    private final MealRepository mealRepository;
    private final MealCategoryMappingRepository mealCategoryMappingRepository;
    private final MemcachedClient memcachedClient;

    public MealService(MealRepository mealRepository, MealCategoryMappingRepository mealCategoryMappingRepository,
                       MemcachedClient memcachedClient) {
        this.mealRepository = mealRepository;
        this.mealCategoryMappingRepository = mealCategoryMappingRepository;
        this.memcachedClient = memcachedClient;
    }

    public Mono<CustomPaginateResponse<List<MealWithCategoriesDTO>>> getAllMeal(Pageable pageable) {
        return mealRepository.count()
                .flatMap(totalCount -> mealRepository.findAllMealsWithCategories()
                        .collectList()
                        .map(paginatedMeals -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount, pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<MealWithCategoriesDTO>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedMeals);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        }))
                .onErrorReturn(getErrorResponse(500, "Get All Meals Error Occurred"));
    }

//    public Mono<CustomResponse<MealCategoryMappingRequest>> getMealById(int mealId) {
//
//        Mono<Meal> mealMono = mealRepository.findById(mealId);
//
//        Flux<Integer> categoryIdsFlux = mealCategoryMappingRepository.findCategoryIdsByMealId(mealId);
//        Mono<List<Integer>> categoryIdsMono = categoryIdsFlux.collectList();
//
//        return Mono.zip(mealMono, categoryIdsMono)
//                .map(tuple -> {
//                    Meal meal = tuple.getT1();
//                    List<Integer> categoryIds = tuple.getT2();
//
//                    MealCategoryMappingRequest response = new MealCategoryMappingRequest();
//                    response.setMealId(meal.getMealId());
//                    response.setName(meal.getName());
//                    response.setCategoryIds(categoryIds);
//                    response.setCreatedBy(meal.getCreatedBy());
//                    response.setCreatedTime(meal.getCreatedTime());
//                    response.setUpdatedBy(meal.getUpdatedBy());
//                    response.setUpdatedTime(meal.getUpdatedTime());
//
//                    return new CustomResponse<>(new Status(200, "Success"), response);
//                })
//                .switchIfEmpty(Mono.just(getNotFoundMealResponse()));
//    }

    public Mono<CustomResponse<MealCategoryMappingRequest>> getMealById(int mealId) {
        Mono<Meal> mealMono = mealRepository.findById(mealId);

        Flux<CategoryWithCount> categoryDataFlux = mealCategoryMappingRepository.findCategoryIdsAndCountByMealId(mealId);
        Mono<List<RecipeSearchResponse>> recipeResponsesMono = categoryDataFlux
                .map(categoryWithCount -> {
                    RecipeSearchResponse response = new RecipeSearchResponse();
                    response.setId(categoryWithCount.getCategoryId());
                    response.setCost(BigDecimal.valueOf(categoryWithCount.getCount()));
                    // Set other properties if needed
                    return response;
                })
                .collectList();

        return Mono.zip(mealMono, recipeResponsesMono)
                .map(tuple -> {
                    Meal meal = tuple.getT1();
                    List<RecipeSearchResponse> recipeResponses = tuple.getT2();

                    MealCategoryMappingRequest response = new MealCategoryMappingRequest();
                    response.setMealId(meal.getMealId());
                    response.setName(meal.getName());
                    response.setCategoryIds(recipeResponses); // Assuming you have a setter for recipeResponses in MealCategoryMappingRequest class
                    response.setCreatedBy(meal.getCreatedBy());
                    response.setCreatedTime(meal.getCreatedTime());
                    response.setUpdatedBy(meal.getUpdatedBy());
                    response.setUpdatedTime(meal.getUpdatedTime());

                    return new CustomResponse<>(new Status(200, "Success"), response);
                })
                .switchIfEmpty(Mono.just(getNotFoundMealResponse()));
    }


    private CustomResponse<MealCategoryMappingRequest> getNotFoundMealResponse() {
        return new CustomResponse<>(new Status(404, "Meal not found"), null);
    }


    public Mono<CustomResponse<Object>> createMeal(MealCategoryMappingRequest createMealRequest) {
        return mealRepository.findByName(createMealRequest.getName())
                .flatMap(existingMeal -> Mono.just(new CustomResponse<>(new Status(403, "Meal name already exists"), null)))
                .switchIfEmpty(Mono.defer(() -> {
                    Meal newMeal = new Meal();
                    newMeal.setName(createMealRequest.getName());
                    newMeal.setActive(true);
                    newMeal.setCreatedBy(createMealRequest.getCreatedBy());
                    newMeal.setCreatedTime(Instant.now());

                    return mealRepository.save(newMeal)
                            .flatMap(savedMeal -> {
                                memcachedClient.set("mealNames-" + savedMeal.getMealId(), 0, savedMeal.getName());

                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("mealEntityList");
                                processData.add(new RecipeSearchResponse(savedMeal.getMealId(),savedMeal.getName(),0,null,null));
                                memcachedClient.set("mealEntityList",0,processData);

                                List<RecipeSearchResponse> categoryIds = createMealRequest.getCategoryIds();
                                List<MealCategoryMapping> mappings = categoryIds.stream()
                                        .map(categoryId -> {
                                            MealCategoryMapping mapping = new MealCategoryMapping();
                                            mapping.setMealId(savedMeal.getMealId());
                                            mapping.setCategoryId(categoryId.getId());
                                            mapping.setCount(categoryId.getCost().intValue());
                                            mapping.setCreatedBy(savedMeal.getCreatedBy());
                                            mapping.setCreatedTime(Instant.now());
                                            return mapping;
                                        })
                                        .collect(Collectors.toList());

                                return mealCategoryMappingRepository.saveAll(mappings)
                                        .collectList()
                                        .flatMap(savedMappings -> {
                                            memcachedClient.set("mealNames-"+savedMeal.getMealId(),0,savedMeal.getName());
                                            return Mono.just(new CustomResponse<>(new Status(201, "Meal created successfully"), savedMeal));
                                        });
                            });
                }))
                .onErrorResume(throwable -> {
                    logger.error("Error creating meal: {}", throwable.getMessage());
                    return Mono.just(new CustomResponse<>(new Status(500, "Failed to create meal"), null));
                });
    }

    public Mono<CustomResponse<Meal>> updateMeal(int mealId, MealCategoryMappingRequest updateMealRequest) {
        return mealRepository.findById(mealId)
                .flatMap(existingMeal -> {
                    existingMeal.setName(updateMealRequest.getName());
                    existingMeal.setUpdatedBy(updateMealRequest.getUpdatedBy());
                    existingMeal.setUpdatedTime(Instant.now());

                    return mealRepository.save(existingMeal)
                            .doOnNext(savedMeal -> {
                                memcachedClient.set("mealNames-" + savedMeal.getMealId(), 0, savedMeal.getName());
                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("mealEntityList");
                                    processData.add(new RecipeSearchResponse(savedMeal.getMealId(),savedMeal.getName(),0,null,null));
                                memcachedClient.set("mealEntityList",0,processData);
                            })
                            .then(mealCategoryMappingRepository.deleteByMealId(existingMeal.getMealId())
                                    .thenMany(Flux.fromIterable(updateMealRequest.getCategoryIds()))
                                    .flatMap(categoryId -> {
                                        MealCategoryMapping mealCategoryMapping = new MealCategoryMapping();
                                        mealCategoryMapping.setMealId(existingMeal.getMealId());
                                        mealCategoryMapping.setCategoryId(categoryId.getId());
                                        mealCategoryMapping.setCount(categoryId.getCost().intValue());
                                        mealCategoryMapping.setCreatedBy(updateMealRequest.getCreatedBy());
                                        mealCategoryMapping.setCreatedTime(Instant.now());
                                        mealCategoryMapping.setUpdatedBy(updateMealRequest.getUpdatedBy());
                                        mealCategoryMapping.setUpdatedTime(Instant.now());
                                        mealCategoryMapping.setActive(true);

                                        return mealCategoryMappingRepository.save(mealCategoryMapping);
                                    })
                                    .then())
                            .thenReturn(new CustomResponse<>(new Status(200, "Meal updated successfully"), existingMeal));
                })
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }

    public Mono<Meal> deleteMeal(int mealId) {
        return mealRepository.findById(mealId)
                .flatMap(meal -> mealRepository.deleteById(mealId).thenReturn(meal))
                .doOnNext(deletedMeal -> memcachedClient.delete("mealNames-"+mealId))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found..!!"))));
    }

    private CustomResponse<Meal> getNotFoundResponse() {
        return new CustomResponse<>(new Status(404, "Meal not found"), null);
    }
}

