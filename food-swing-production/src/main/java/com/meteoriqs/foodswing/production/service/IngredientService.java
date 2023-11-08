package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.entity.StockAudit;
import com.meteoriqs.foodswing.data.model.PaginationInfo;
import com.meteoriqs.foodswing.data.model.RecipeSearchResponse;
import com.meteoriqs.foodswing.data.model.Status;
import com.meteoriqs.foodswing.data.model.Ingredients;
import com.meteoriqs.foodswing.data.repository.IngredientsRepository;
import com.meteoriqs.foodswing.data.repository.StockAuditRepository;
import net.spy.memcached.MemcachedClient;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class IngredientService {

    private final IngredientsRepository ingredientsRepository;
    private final StockAuditRepository stockAuditRepository;
    private final MemcachedClient memcachedClient;

    public IngredientService(IngredientsRepository ingredientsRepository, StockAuditRepository stockAuditRepository,
                             MemcachedClient memcachedClient) {
        this.ingredientsRepository = ingredientsRepository;
        this.stockAuditRepository = stockAuditRepository;
        this.memcachedClient = memcachedClient;
    }


    //TODO: Handle ingredient cost becoz after del cost cannot get

    public Mono<CustomPaginateResponse<List<Ingredients>>> getAllIngredientsSample(Pageable pageable) {
        Mono<Long> totalCountMono = ingredientsRepository.count();
        Flux<Ingredients> paginatedIngredientsFlux = ingredientsRepository.findAllWithPagination(pageable);
        return totalCountMono.zipWith(paginatedIngredientsFlux.collectList())
                .map(tuple -> {
                    long totalCount = tuple.getT1();
                    List<Ingredients> paginatedIngredients = tuple.getT2();
                    PaginationInfo paginationInfo = new PaginationInfo(totalCount, pageable.getPageNumber() + 1, pageable.getPageSize());
                    CustomPaginateResponse<List<Ingredients>> response = new CustomPaginateResponse<>();
                    response.setStatus(new Status(200, "Success"));
                    response.setData(paginatedIngredients);
                    response.setPaginationInfo(paginationInfo);
                    return response;
                })
                .onErrorResume(throwable -> {
                    CustomPaginateResponse<List<Ingredients>> errorResponse = new CustomPaginateResponse<>();
                    errorResponse.setStatus(new Status(500, "Error:" + throwable.getMessage()));
                    return Mono.just(errorResponse);
                });
    }

    public Mono<CustomPaginateResponse<List<Ingredients>>> getAllIngredients(Pageable pageable) {
        Mono<Long> totalCountMono = ingredientsRepository.count();
        Flux<Ingredients> paginatedIngredientsFlux = ingredientsRepository.findAllWithPagination(pageable);

        return totalCountMono.flatMap(totalCount -> paginatedIngredientsFlux.collectList()
                .flatMap(paginatedIngredients -> {
                    // Populate uomName for each ingredient from the configuration
                    return Flux.fromIterable(paginatedIngredients)
                            .flatMap(ingredient -> {
                                int uomId = ingredient.getUomId();
                                String uomName = (String) memcachedClient.get("uomNames-" + uomId);
                                ingredient.setUomName(uomName);
                                return Mono.just(ingredient);
                            })
                            .collectList()
                            .map(updatedIngredients -> {
                                PaginationInfo paginationInfo = new PaginationInfo(totalCount, pageable.getPageNumber() + 1, pageable.getPageSize());
                                CustomPaginateResponse<List<Ingredients>> response = new CustomPaginateResponse<>();
                                response.setStatus(new Status(200, "Success"));
                                response.setData(updatedIngredients);
                                response.setPaginationInfo(paginationInfo);
                                return response;
                            });
                })
                .onErrorResume(throwable -> {
                    CustomPaginateResponse<List<Ingredients>> errorResponse = new CustomPaginateResponse<>();
                    errorResponse.setStatus(new Status(500, "Error: " + throwable.getMessage()));
                    return Mono.just(errorResponse);
                })
        );
    }

    public Mono<CustomResponse<Ingredients>> getIngredientById(int ingredientId) {
        return ingredientsRepository.findById(ingredientId)
                .map(ingredient -> {
                    CustomResponse<Ingredients> response = new CustomResponse<>();
                    response.setStatus(new Status(200, "Success"));
                    response.setData(ingredient);
                    return response;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    CustomResponse<Ingredients> notFoundResponse = new CustomResponse<>();
                    notFoundResponse.setStatus(new Status(404, "Ingredient not found"));
                    return Mono.just(notFoundResponse);
                }))
                .onErrorResume(throwable -> {
                    CustomResponse<Ingredients> errorResponse = new CustomResponse<>();
                    errorResponse.setStatus(new Status(500, "Error: " + throwable.getMessage()));
                    return Mono.just(errorResponse);
                });
    }

    public Mono<CustomResponse<Boolean>> deleteIngredientById(int ingredientId) {
        return ingredientsRepository.findById(ingredientId)
                .flatMap(deletedIngredient -> {
                    memcachedClient.delete("ingredientNames-" + deletedIngredient.getIngredientId());
                    memcachedClient.delete("ingredientUomMapper-" + deletedIngredient.getUomId());
                    memcachedClient.delete("ingredientCost-" + deletedIngredient.getCost());
                    return ingredientsRepository.deleteById(ingredientId).thenReturn(deletedIngredient);
                })
                .thenReturn(createCustomResponse(201, "Ingredient Deleted Successfully"))
                .defaultIfEmpty(createCustomResponse(404, "Ingredient Not Found"));
    }

    private CustomResponse<Boolean> createCustomResponse(int code, String message) {
        CustomResponse<Boolean> response = new CustomResponse<>();
        response.setStatus(new Status(code, message));
        response.setData(Boolean.TRUE);
        return response;
    }

    public Mono<CustomResponse<Object>> createIngredient(Ingredients createIngredientRequest) {
        return ingredientsRepository.findByIngredientName(createIngredientRequest.getIngredientName())
                .flatMap(existingUom -> Mono.just(new CustomResponse<>(new Status(403, "Ingredient name already exists"), null)))
                .switchIfEmpty(Mono.defer(() -> {
                    Ingredients newIngredient = new Ingredients();
                    newIngredient.setIngredientName(createIngredientRequest.getIngredientName());
                    newIngredient.setUomId(createIngredientRequest.getUomId());
                    newIngredient.setCost(createIngredientRequest.getCost());
                    newIngredient.setActive(true);
                    newIngredient.setCreatedBy(createIngredientRequest.getCreatedBy());
                    newIngredient.setCreatedTime(Instant.now());
                    return ingredientsRepository.save(newIngredient)
                            .flatMap(savedIngredient -> {
                                String uomName = (String) memcachedClient.get("uomNames-" + savedIngredient.getUomId());
                                memcachedClient.set("ingredientNames-" + savedIngredient.getIngredientId(), 0, savedIngredient.getIngredientName());
                                memcachedClient.set("ingredientUomMapper-" + savedIngredient.getIngredientId(), 0, savedIngredient.getUomId());
                                memcachedClient.set("ingredientCost-" + savedIngredient.getIngredientId(), 0, savedIngredient.getCost());

                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("ingredientEntityList");
                                processData.add(new RecipeSearchResponse(savedIngredient.getIngredientId(), savedIngredient.getIngredientName(), savedIngredient.getUomId(), uomName, savedIngredient.getCost()));
                                memcachedClient.set("ingredientEntityList", 0, processData);

                                return Mono.just(new CustomResponse<>(new Status(201, "Ingredient created successfully"), savedIngredient));
                            });
                }));
    }

    public Mono<CustomResponse<Ingredients>> updateIngredient(int ingredientId, Ingredients updatedIngredient) {
        CustomResponse<Ingredients> response = new CustomResponse<>();
        return ingredientsRepository.findById(ingredientId)
                .flatMap(existingIngredient -> {
                    existingIngredient.setIngredientName(updatedIngredient.getIngredientName());
                    existingIngredient.setUomId(updatedIngredient.getUomId());
                    existingIngredient.setCost(updatedIngredient.getCost());
                    existingIngredient.setUpdatedBy(updatedIngredient.getUpdatedBy());
                    existingIngredient.setUpdatedTime(Instant.now());
                    return ingredientsRepository.save(existingIngredient)
                            .map(savedIngredient -> {
                                String uomName = (String) memcachedClient.get("uomNames-" + savedIngredient.getUomId());
                                memcachedClient.set("ingredientNames-" + savedIngredient.getIngredientId(), 0, savedIngredient.getIngredientName());
                                memcachedClient.set("ingredientUomMapper-" + savedIngredient.getIngredientId(), 0, savedIngredient.getUomId());
                                memcachedClient.set("ingredientCost-" + savedIngredient.getIngredientId(), 0, savedIngredient.getCost());
                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("ingredientEntityList");
                                processData.add(new RecipeSearchResponse(savedIngredient.getIngredientId(), savedIngredient.getIngredientName(), savedIngredient.getUomId(), uomName, savedIngredient.getCost()));
                                memcachedClient.set("ingredientEntityList", 0, processData);
                                response.setStatus(new Status(200, "Ingredient Updated Successfully"));
                                response.setData(savedIngredient);
                                return response;
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    response.setStatus(new Status(500, "Ingredient not Found"));
                    return Mono.just(response);
                }));
    }

    public Mono<CustomPaginateResponse<List<Ingredients>>> getAllIngredientsStock(Pageable pageable) {
        Mono<Long> totalCountMono = ingredientsRepository.count();
        Flux<Ingredients> paginatedIngredientsFlux = ingredientsRepository.findAllWithPagination(pageable);

        return totalCountMono.flatMap(totalCount -> paginatedIngredientsFlux.collectList()
                .flatMap(paginatedIngredients -> {
                    // Populate uomName for each ingredient from the configuration
                    return Flux.fromIterable(paginatedIngredients)
                            .flatMap(ingredient -> {
                                int uomId = ingredient.getUomId();
                                String uomName = (String) memcachedClient.get("uomNames-" + uomId);
                                ingredient.setUomName(uomName);
                                ingredient.setCount(BigDecimal.valueOf(0));
                                return Mono.just(ingredient);
                            })
                            .collectList()
                            .map(updatedIngredients -> {
                                PaginationInfo paginationInfo = new PaginationInfo(totalCount, pageable.getPageNumber() + 1, pageable.getPageSize());
                                CustomPaginateResponse<List<Ingredients>> response = new CustomPaginateResponse<>();
                                response.setStatus(new Status(200, "Success"));
                                response.setData(updatedIngredients);
                                response.setPaginationInfo(paginationInfo);
                                return response;
                            });
                })
                .onErrorResume(throwable -> {
                    CustomPaginateResponse<List<Ingredients>> errorResponse = new CustomPaginateResponse<>();
                    errorResponse.setStatus(new Status(500, "Error: " + throwable.getMessage()));
                    return Mono.just(errorResponse);
                })
        );
    }


    public Mono<CustomResponse<List<Ingredients>>> updateIngredientStock(List<Ingredients> updatedIngredients) {
        return Flux.fromIterable(updatedIngredients)
                .flatMap(updatedIngredient -> ingredientsRepository.findById(updatedIngredient.getIngredientId())
                        .flatMap(existingIngredient -> {
                            existingIngredient.setUpdatedBy(updatedIngredient.getUpdatedBy());
                            existingIngredient.setUpdatedTime(Instant.now());
                            existingIngredient.setStock(updatedIngredient.getCount());
                            return ingredientsRepository.save(existingIngredient)
                                    .flatMap(savedIngredient -> {
                                        String uomName = (String) memcachedClient.get("uomNames-" + savedIngredient.getUomId());
                                        memcachedClient.set("ingredientNames-" + savedIngredient.getIngredientId(), 0, savedIngredient.getIngredientName());
                                        memcachedClient.set("ingredientUomMapper-" + savedIngredient.getIngredientId(), 0, savedIngredient.getUomId());
                                        memcachedClient.set("ingredientCost-" + savedIngredient.getIngredientId(), 0, savedIngredient.getCost());
                                        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("ingredientEntityList");
                                        processData.add(new RecipeSearchResponse(savedIngredient.getIngredientId(), savedIngredient.getIngredientName(), savedIngredient.getUomId(), uomName, savedIngredient.getCost()));
                                        memcachedClient.set("ingredientEntityList", 0, processData);

                                        StockAudit stockAudit = new StockAudit();
                                        stockAudit.setIngredientId(savedIngredient.getIngredientId());
                                        stockAudit.setIngredientQty(updatedIngredient.getCount());
                                        stockAudit.setPrice(savedIngredient.getCost());
                                        stockAudit.setCreatedBy(savedIngredient.getCreatedBy());
                                        stockAudit.setCreatedTime(Instant.now());
                                        return stockAuditRepository.save(stockAudit).thenReturn(savedIngredient);
                                    });
                        })
                        .switchIfEmpty(Mono.just(updatedIngredient))
                )
                .collectList()
                .map(updatedList -> {
                    CustomResponse<List<Ingredients>> response = new CustomResponse<>();
                    response.setStatus(new Status(200, "Ingredient Stock Updated Successfully"));
                    response.setData(updatedList);
                    return response;
                });
    }


    public Mono<CustomResponse<List<Ingredients>>> updateIngredientWastage(List<Ingredients> updatedIngredients) {
        return Flux.fromIterable(updatedIngredients)
                .flatMap(updatedIngredient -> ingredientsRepository.findById(updatedIngredient.getIngredientId())
                        .flatMap(existingIngredient -> {
                            existingIngredient.setUpdatedBy(updatedIngredient.getUpdatedBy());
                            existingIngredient.setUpdatedTime(Instant.now());

                            // Calculate the new stock value (stock - count)
                            BigDecimal newStock = updatedIngredient.getStock().subtract(updatedIngredient.getCount());
                            existingIngredient.setStock(newStock);

                            return ingredientsRepository.save(existingIngredient)
                                    .flatMap(savedIngredient -> {
                                        String uomName = (String) memcachedClient.get("uomNames-" + savedIngredient.getUomId());
                                        memcachedClient.set("ingredientNames-" + savedIngredient.getIngredientId(), 0, savedIngredient.getIngredientName());
                                        memcachedClient.set("ingredientUomMapper-" + savedIngredient.getIngredientId(), 0, savedIngredient.getUomId());
                                        memcachedClient.set("ingredientCost-" + savedIngredient.getIngredientId(), 0, savedIngredient.getCost());
                                        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("ingredientEntityList");
                                        processData.add(new RecipeSearchResponse(savedIngredient.getIngredientId(), savedIngredient.getIngredientName(), savedIngredient.getUomId(), uomName, savedIngredient.getCost()));
                                        memcachedClient.set("ingredientEntityList", 0, processData);

                                        StockAudit stockAudit = new StockAudit();
                                        stockAudit.setIngredientId(savedIngredient.getIngredientId());
                                        stockAudit.setIngredientQty(newStock);
                                        stockAudit.setWastageQty(updatedIngredient.getCount());
                                        stockAudit.setCreatedBy(savedIngredient.getCreatedBy());
                                        stockAudit.setCreatedTime(Instant.now());
                                        return stockAuditRepository.save(stockAudit).thenReturn(savedIngredient);
                                    });
                        })
                        .switchIfEmpty(Mono.just(updatedIngredient))
                )
                .collectList()
                .map(updatedList -> {
                    CustomResponse<List<Ingredients>> response = new CustomResponse<>();
                    response.setStatus(new Status(200, "Ingredient Stock Updated Successfully"));
                    response.setData(updatedList);
                    return response;
                });
    }

    public Mono<CustomResponse<List<Ingredients>>> updateIngredientUsed(List<Ingredients> updatedIngredients) {
        return Flux.fromIterable(updatedIngredients)
                .flatMap(updatedIngredient -> ingredientsRepository.findById(updatedIngredient.getIngredientId())
                        .flatMap(existingIngredient -> {
                            existingIngredient.setUpdatedBy(updatedIngredient.getUpdatedBy());
                            existingIngredient.setUpdatedTime(Instant.now());

                            // Calculate the new stock value (stock - count)
                            BigDecimal newStock = updatedIngredient.getStock().subtract(updatedIngredient.getCount());
                            existingIngredient.setStock(newStock);

                            return ingredientsRepository.save(existingIngredient)
                                    .flatMap(savedIngredient -> {
                                        String uomName = (String) memcachedClient.get("uomNames-" + savedIngredient.getUomId());
                                        memcachedClient.set("ingredientNames-" + savedIngredient.getIngredientId(), 0, savedIngredient.getIngredientName());
                                        memcachedClient.set("ingredientUomMapper-" + savedIngredient.getIngredientId(), 0, savedIngredient.getUomId());
                                        memcachedClient.set("ingredientCost-" + savedIngredient.getIngredientId(), 0, savedIngredient.getCost());
                                        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("ingredientEntityList");
                                        processData.add(new RecipeSearchResponse(savedIngredient.getIngredientId(), savedIngredient.getIngredientName(), savedIngredient.getUomId(), uomName, savedIngredient.getCost()));
                                        memcachedClient.set("ingredientEntityList", 0, processData);

                                        StockAudit stockAudit = new StockAudit();
                                        stockAudit.setIngredientId(savedIngredient.getIngredientId());
                                        stockAudit.setIngredientQty(newStock);
                                        stockAudit.setUsedQty(updatedIngredient.getCount());
                                        stockAudit.setCreatedBy(savedIngredient.getCreatedBy());
                                        stockAudit.setCreatedTime(Instant.now());
                                        return stockAuditRepository.save(stockAudit).thenReturn(savedIngredient);
                                    });
                        })
                        .switchIfEmpty(Mono.just(updatedIngredient))
                )
                .collectList()
                .map(updatedList -> {
                    CustomResponse<List<Ingredients>> response = new CustomResponse<>();
                    response.setStatus(new Status(200, "Ingredient Stock Updated Successfully"));
                    response.setData(updatedList);
                    return response;
                });
    }



}
