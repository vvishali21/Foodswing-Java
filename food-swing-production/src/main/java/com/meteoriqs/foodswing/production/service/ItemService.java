package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.data.repository.*;
import lombok.extern.log4j.Log4j2;
import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ItemService extends BaseService {

    private final ItemRepository itemRepository;
    private final GrammageRepository grammageRepository;
    private final MealItemMappingRepository mealItemMappingRepository;
    private final UomMasterRepository uomMasterRepository;
    private final MemcachedClient memcachedClient;

    @Autowired
    public ItemService(ItemRepository itemRepository, GrammageRepository grammageRepository,
                       MealItemMappingRepository mealItemMappingRepository,
                       MemcachedClient memcachedClient, UomMasterRepository uomMasterRepository) {
        this.itemRepository = itemRepository;
        this.grammageRepository = grammageRepository;
        this.mealItemMappingRepository = mealItemMappingRepository;
        this.uomMasterRepository = uomMasterRepository;
        this.memcachedClient = memcachedClient;
    }

    public Mono<CustomPaginateResponse<List<ItemWithDetailsDTO>>> getAllItems(Pageable pageable) {
        return itemRepository.findAllItemsWithDetails()
                .collectList()
                .flatMap(paginatedItems -> itemRepository.count()
                        .map(totalCount -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount, pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<ItemWithDetailsDTO>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedItems);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        }))
//                .onErrorReturn(getErrorResponse(500, "Get All Items Error Occurred"));
                .onErrorResume(error -> {
                    log.error("Error occurred while fetching items with details", error);
                    return Mono.just(getErrorResponse(500, "Get All Items Error Occurred"));
                });
    }

    public CustomPaginateResponse<List<ItemWithDetailsDTO>> getErrorResponse(int statusCode, String message) {
        CustomPaginateResponse<List<ItemWithDetailsDTO>> errorResponse = new CustomPaginateResponse<>();
        errorResponse.setStatus(new Status(statusCode, message));
        return errorResponse;
    }



    public Mono<CustomResponse<ItemMealGramResponse>> getItemById(int itemId) {

        Mono<Item> itemMono = itemRepository.findById(itemId);

        Flux<Integer> mealIdFlux = mealItemMappingRepository.findMealIdsByItemId(itemId);
        Mono<List<Integer>> mealListMono = mealIdFlux.collectList();
        Mono<BigDecimal> gramMono = grammageRepository.findGramByItemId(itemId);

        Flux<Integer> categoryIdsFlux = mealItemMappingRepository.findCategoryIdsByItemId(itemId);
        Mono<List<Integer>> categoryIdsMono = categoryIdsFlux.collectList();

        return Mono.zip(itemMono, mealListMono, gramMono, categoryIdsMono)
                .map(tuple -> {
                    Item item = tuple.getT1();
                    List<Integer> mealList = tuple.getT2();
                    BigDecimal gram = tuple.getT3();
                    List<Integer> categoryIds = tuple.getT4();

                    Integer categoryId = categoryIds.isEmpty() ? null : categoryIds.get(0);

                    ItemMealGramResponse response = new ItemMealGramResponse();
                    response.setItemId(item.getItemId());
                    response.setItemName(item.getItemName());
                    response.setUomId(item.getUomId());
                    response.setCategoryId(categoryId);
                    response.setCategoryIds(categoryIds);
                    response.setGram(gram);
                    response.setWeight(item.getWeight());
                    response.setMealList(mealList);
                    response.setCreatedBy(item.getCreatedBy());
                    response.setCreatedTime(item.getCreatedTime());
                    response.setUpdatedBy(item.getUpdatedBy());
                    response.setUpdatedTime(item.getUpdatedTime());

                    return new CustomResponse<>(new Status(200, "Success"), response);
                })
                .switchIfEmpty(Mono.just(getNotFoundResponses()));
    }

    private CustomResponse<ItemMealGramResponse> getNotFoundResponses() {
        return new CustomResponse<>(new Status(404, "Item not found"), null);
    }

    public Mono<CustomResponse<Object>> createItem(ItemMealGramResponse request) {
        return itemRepository.findByItemName(request.getItemName())
                .flatMap(existingItem -> Mono.just(new CustomResponse<>(new Status
                        (403, "Item name already exists"), null)))
                .switchIfEmpty(Mono.defer(() -> {
                    // Fetch the UOM name based on request.getUomId()
                    return uomMasterRepository.findById(request.getUomId())
                            .flatMap(uomEntity -> {
                                Item newItem = new Item();
                                newItem.setItemName(request.getItemName());
                                newItem.setUomId(request.getUomId());
                                newItem.setCategoryId(request.getCategoryId());
                                newItem.setGram(request.getGram());

                                // Determine the default weight based on UOM name
                                String uomName = uomEntity.getName();
                                if ("KG".equalsIgnoreCase(uomName) || "Litre".equalsIgnoreCase(uomName)) {
                                    newItem.setWeight(new BigDecimal("1000"));
                                } else {
                                    newItem.setWeight(request.getWeight());
                                }

                                newItem.setActive(true);
                                newItem.setCreatedBy(request.getCreatedBy());
                                newItem.setCreatedTime(Instant.now());

                                return itemRepository.save(newItem)
                                        .flatMap(savedItem -> {
                                            memcachedClient.set("itemNames-" + savedItem.getItemId(), 0, savedItem.getItemName());
                                            memcachedClient.set("itemUomMapper-" + savedItem.getItemId(), 0, savedItem.getUomId());
                                            memcachedClient.set("itemCategoryMapper-" + savedItem.getItemId(), 0, savedItem.getCategoryId());
                                            memcachedClient.set("itemGramMapper-" + savedItem.getItemId(), 0, savedItem.getGram());

                                            List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("itemEntityList");
                                                processData.add(new RecipeSearchResponse(savedItem.getItemId(),savedItem.getItemName(),0,null,null));
                                                memcachedClient.set("itemEntityList",0,processData);

                                            log.info("Item Service :: Save :: itemUomMapper :: {}", memcachedClient.get("itemUomMapper-"));
                                            // Create and save MealItemMapping records
                                            List<MealItemMapping> mealItemMappings = request.getMealList().stream()
                                                    .map(mealId -> {
                                                        MealItemMapping mapping = new MealItemMapping();
                                                        mapping.setMealId(mealId);
                                                        mapping.setItemId(savedItem.getItemId());
                                                        mapping.setCategoryId(request.getCategoryId());
                                                        mapping.setActive(true);
                                                        mapping.setCreatedBy(request.getCreatedBy());
                                                        mapping.setCreatedTime(Instant.now());
                                                        return mapping;
                                                    })
                                                    .collect(Collectors.toList());

                                            return mealItemMappingRepository.saveAll(mealItemMappings)
                                                    .collectList()
                                                    .flatMap(savedMappings -> {

                                                        // Fetch a list of companies from ConfigurationHolder
                                                        List<RecipeSearchResponse> companyData = (List<RecipeSearchResponse>) memcachedClient.get("companyEntityList");
                                                        // Fetch a list of companies from ConfigurationHolder
                                                        // Create and save Grammage records for each company, mealId, and day
                                                        List<Grammage> grammageRecords = new ArrayList<>();
                                                        for (RecipeSearchResponse data : companyData) {
                                                            for (Integer mealId : request.getMealList()) {
                                                                for (int day = 0; day <= 6; day++) {
                                                                    Grammage grammage = new Grammage();
                                                                    grammage.setCompanyId(data.getId());
                                                                    grammage.setMealId(mealId);
                                                                    grammage.setItemId(savedItem.getItemId());
                                                                    grammage.setDay(day);
                                                                    grammage.setGram(request.getGram());
                                                                    grammage.setActive(true);
                                                                    grammage.setCreatedBy(request.getCreatedBy());
                                                                    grammage.setCreatedTime(Instant.now());
                                                                    grammageRecords.add(grammage);
                                                                }
                                                            }
                                                        }
                                                        return grammageRepository.saveAll(grammageRecords)
                                                                .collectList()
                                                                .map(savedGrammageList -> new CustomResponse<>(new Status
                                                                        (201, "Item created successfully"),
                                                                        (Object) savedGrammageList));
                                                    });
                                        })
                                        .switchIfEmpty(Mono.just(new CustomResponse<>(new Status(500,
                                                "Failed to create item"), (Object) null)));
                            });
                }));
    }


    public Mono<CustomResponse<Item>> updateItem(int itemId, ItemMealGramResponse updateItemRequest) {
        //TODO: item update handle it add one more meal type id
        return itemRepository.findById(itemId)
                .flatMap(existingItem -> {
                    existingItem.setItemName(updateItemRequest.getItemName());
                    existingItem.setUomId(updateItemRequest.getUomId());
                    existingItem.setCategoryId(updateItemRequest.getCategoryId());
                    existingItem.setGram(updateItemRequest.getGram());
                    existingItem.setWeight(updateItemRequest.getWeight());
                    existingItem.setUpdatedBy(updateItemRequest.getUpdatedBy());
                    existingItem.setUpdatedTime(Instant.now());

                    return itemRepository.save(existingItem)
                            .doOnNext(savedItem -> {
                                memcachedClient.set("itemNames-"+savedItem.getItemId(), 0, savedItem.getItemName());
                                memcachedClient.set("itemUomMapper-"+savedItem.getItemId(),0,savedItem.getUomId());
                                memcachedClient.set("itemCategoryMapper-"+savedItem.getItemId(),0,savedItem.getCategoryId());
                                memcachedClient.set("itemGramMapper-"+savedItem.getItemId(),0,savedItem.getGram());

                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("itemEntityList");
                                processData.add(new RecipeSearchResponse(savedItem.getItemId(),savedItem.getItemName(),0,null,null));
                                memcachedClient.set("itemEntityList",0,processData);

                            })
                            .then(mealItemMappingRepository.deleteByItemId(existingItem.getItemId())
                                    .thenMany(Flux.fromIterable(updateItemRequest.getMealList()))
                                    .flatMap(mealId -> {
                                        MealItemMapping mealItemMapping = new MealItemMapping();
                                        mealItemMapping.setMealId(mealId);
                                        mealItemMapping.setItemId(existingItem.getItemId());
                                        mealItemMapping.setCategoryId(updateItemRequest.getCategoryId());
                                        mealItemMapping.setCreatedBy(updateItemRequest.getCreatedBy());
                                        mealItemMapping.setCreatedTime(Instant.now());
                                        mealItemMapping.setUpdatedBy(updateItemRequest.getUpdatedBy());
                                        mealItemMapping.setUpdatedTime(Instant.now());
                                        mealItemMapping.setActive(true);

                                        return mealItemMappingRepository.save(mealItemMapping);
                                    })
                                    .then()
                            )
                            .thenReturn(new CustomResponse<>(new Status(200, "Item updated successfully"), existingItem));
                })
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }

    public Mono<Item> deleteItem(int itemId) {
        return itemRepository.findById(itemId)
                .flatMap(deletedItem -> {
                    memcachedClient.delete("itemNames-"+deletedItem.getItemId());
                    memcachedClient.delete("itemUomMapper-"+deletedItem.getUomId());
                    return itemRepository.deleteById(itemId).thenReturn(deletedItem);
                })
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found..!!"))));
    }

    private CustomResponse<Item> getNotFoundResponse() {
        return new CustomResponse<>(new Status(404, "Item not found"), null);
    }
}

