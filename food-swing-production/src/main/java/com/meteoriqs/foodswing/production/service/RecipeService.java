package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.entity.*;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.data.model.HopperIndentResponse;
import com.meteoriqs.foodswing.data.model.RecipeGetResponseModel.RecipeGetDetailsResponse;
import com.meteoriqs.foodswing.data.model.RecipeGetResponseModel.Stage;
import com.meteoriqs.foodswing.data.model.RecipeGetResponseModel.StageDetails;
import com.meteoriqs.foodswing.data.model.RecipeGetResponseModel.VesselConfiguration;
import com.meteoriqs.foodswing.data.repository.*;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private final RecipeMasterRepository recipeMasterRepository;
    private final HopperIndentRepository hopperIndentRepository;
    private final HopperItemIndentRepository hopperItemIndentRepository;
    private final PreparationIndentRepository preparationIndentRepository;
    private final VesselIndentRepository vesselIndentRepository;
    private final ItemRepository itemRepository;
    private final UomMasterRepository uomMasterRepository;
    private final RecipeDetailsRepository recipeDetailsRepository;
    private final IngredientsRepository ingredientsRepository;
    private final ProcessMasterRepository processMasterRepository;
    private final MediumMasterRepository mediumMasterRepository;
    private final MemcachedClient memcachedClient;
    private final OrderMasterRepository orderMasterRepository;
    private final MealSuggestionDetailsRepository mealSuggestionDetailsRepository;
    private final GrammageRepository grammageRepository;
    private final VesselItemIndentRepository vesselItemIndentRepository;


    @Autowired
    public RecipeService(
            RecipeMasterRepository recipeMasterRepository, ItemRepository itemRepository,
            UomMasterRepository uomMasterRepository, RecipeDetailsRepository recipeDetailsRepository,
            IngredientsRepository ingredientsRepository, ProcessMasterRepository processMasterRepository,
            MediumMasterRepository mediumMasterRepository, PreparationIndentRepository preparationIndentRepository,
            HopperIndentRepository hopperIndentRepository, VesselIndentRepository vesselIndentRepository,
            HopperItemIndentRepository hopperItemIndentRepository,
            MemcachedClient memcachedClient, OrderMasterRepository orderMasterRepository,
            MealSuggestionDetailsRepository mealSuggestionDetailsRepository, GrammageRepository grammageRepository, VesselItemIndentRepository vesselItemIndentRepository) {

        this.recipeMasterRepository = recipeMasterRepository;
        this.itemRepository = itemRepository;
        this.uomMasterRepository = uomMasterRepository;
        this.recipeDetailsRepository = recipeDetailsRepository;
        this.ingredientsRepository = ingredientsRepository;
        this.processMasterRepository = processMasterRepository;
        this.mediumMasterRepository = mediumMasterRepository;
        this.preparationIndentRepository = preparationIndentRepository;
        this.hopperIndentRepository = hopperIndentRepository;
        this.vesselIndentRepository = vesselIndentRepository;
        this.hopperItemIndentRepository = hopperItemIndentRepository;
        this.memcachedClient = memcachedClient;
        this.orderMasterRepository = orderMasterRepository;
        this.mealSuggestionDetailsRepository = mealSuggestionDetailsRepository;
        this.grammageRepository = grammageRepository;
        this.vesselItemIndentRepository = vesselItemIndentRepository;
    }

    public Mono<CustomResponse<RecipeGetDetailsResponse>> findRecipeById(int recipeId) {
        Mono<RecipeMaster> masterMono = recipeMasterRepository.findById(recipeId);
        Flux<RecipeDetailsEntity> detailsFlux = recipeDetailsRepository.findByRecipeId(recipeId);
        Status status = new Status(200, "Success");
        Status errorStatus = new Status(500, "Error");

        return masterMono.zipWith(detailsFlux.collectList())
                .flatMap(tuple -> {
                    RecipeGetDetailsResponse response = createRecipeGetDetailsResponse(tuple.getT1(), tuple.getT2());
                    CustomResponse<RecipeGetDetailsResponse> customResponse = new CustomResponse<>();
                    customResponse.setStatus(status);
                    customResponse.setData(response);

                    return Mono.just(customResponse);
                })
                .switchIfEmpty(Mono.just(new CustomResponse<>(errorStatus, null)));
    }


    public Flux<PreparationIndentResponse> getPreparationIndent(String orderDate, int mealId) {
        return preparationIndentRepository.getPreIndent(orderDate, mealId);
    }


    public Flux<HopperIndentResponse> getHopperIndent(String orderDate, int mealId) {
        return hopperIndentRepository.getHopperIndent(orderDate, mealId);
    }


    public Flux<HopperIndentItemResponse> getHopperItemIndent(String orderDate, int mealId) {
        return hopperItemIndentRepository.getHopperItemIndent(orderDate, mealId);
    }


    public Flux<VesselTransformedIndentResponse> getVesselIndent(String orderDate, int mealId) {
        return vesselIndentRepository.getVesselIndent(orderDate, mealId)
                .groupBy(result -> new VesselIndentStageResponse(result.getStage(), result.getItemName(), result.getProcessName()))
                .flatMap(this::transformToTransformedVesselResult);
    }

    public Flux<VesselItemTransformedIndentResponse> getVesselItemIndent(String orderDate, int mealId) {
        return vesselItemIndentRepository.getVesselItemIndent(orderDate, mealId)
                .groupBy(result -> new VesselItemIndentStageResponse(result.getStage(), result.getItemName(), result.getProcessName(), result.getVesselIds(), result.getVesselNames()))
                .flatMap(this::transformToTransformedItemVesselResult);
    }


    public Flux<VesselTransformedIndentResponse> getUnSentVesselIndent() {
        return vesselIndentRepository.getUnSentVesselIndent()
                .groupBy(result -> new VesselIndentStageResponse(result.getStage(), result.getItemName(), result.getProcessName()))
                .flatMap(this::transformToTransformedVesselResult)
                .collectList()
                .flatMapMany(transformedResults -> {
                    if (!transformedResults.isEmpty()) {
                        // Mark the fetched data as 'sent' by updating the 'indent_sent' column
                        return vesselIndentRepository.markSentOrders()
                                .thenMany(Flux.fromIterable(transformedResults));
                    } else {
                        return Flux.empty(); // No data found, return an empty Flux
                    }
                });
    }


    private Flux<VesselItemTransformedIndentResponse> transformToTransformedItemVesselResult(GroupedFlux<VesselItemIndentStageResponse, VesselItemIndentResponse> groupedFlux) {
        return groupedFlux.collectList()
                .flatMapMany(list -> {
                    VesselItemIndentStageResponse stage = groupedFlux.key();
                    VesselItemTransformedIndentResponse result = new VesselItemTransformedIndentResponse();
                    result.setStage(stage);
                    result.setIngredients(list.stream()
                            .map(dto -> {
                                VesselIngredientsIndent ingredients = new VesselIngredientsIndent();
                                ingredients.setMediumId(dto.getMediumId());
                                ingredients.setMediumName(dto.getMediumName());
                                ingredients.setIngredientId(dto.getIngredientId());
                                ingredients.setIngredientName(dto.getIngredientName());
                                ingredients.setQuantity(dto.getQuantity());
                                ingredients.setName(dto.getName());
                                ingredients.setBasePv(dto.getBasePv());
                                ingredients.setBaseSv(dto.getBaseSv());
                                ingredients.setFq(dto.getFq());
                                ingredients.setIngredientCost(dto.getIngredientCost());
                                ingredients.setStageEndAlertDuration(dto.getStageEndAlertDuration());
                                ingredients.setProductionSv(dto.getProductionSv());
                                ingredients.setProductPv(dto.getProductPv());
                                ingredients.setDurationUnit(dto.getDurationUnit());
                                ingredients.setDuration(dto.getDuration());
                                ingredients.setPower(dto.getPower());
                                ingredients.setFwdTime(dto.getFwdTime());
                                ingredients.setRevTime(dto.getRevTime());
                                ingredients.setTimeTaken(dto.getTimeTaken());
                                ingredients.setStageDuration(dto.getStageDuration());
                                return ingredients;
                            })
                            .collect(Collectors.toList()));
                    return Mono.just(result);
                });
    }



    private Flux<VesselTransformedIndentResponse> transformToTransformedVesselResult(GroupedFlux<VesselIndentStageResponse, VesselIndentResponse> groupedFlux) {
        return groupedFlux.collectList()
                .flatMapMany(list -> {
                    VesselIndentStageResponse stage = groupedFlux.key();
                    VesselTransformedIndentResponse result = new VesselTransformedIndentResponse();
                    result.setStage(stage);
                    result.setIngredients(list.stream()
                            .map(dto -> {
                                VesselIngredientsIndent ingredients = new VesselIngredientsIndent();
                                ingredients.setMediumId(dto.getMediumId());
                                ingredients.setMediumName(dto.getMediumName());
                                ingredients.setIngredientId(dto.getIngredientId());
                                ingredients.setIngredientName(dto.getIngredientName());
                                ingredients.setQuantity(dto.getQuantity());
                                ingredients.setName(dto.getName());
                                ingredients.setBasePv(dto.getBasePv());
                                ingredients.setBaseSv(dto.getBaseSv());
                                ingredients.setFq(dto.getFq());
                                ingredients.setIngredientCost(dto.getIngredientCost());
                                ingredients.setStageEndAlertDuration(dto.getStageEndAlertDuration());
                                ingredients.setProductionSv(dto.getProductionSv());
                                ingredients.setProductPv(dto.getProductPv());
                                ingredients.setDurationUnit(dto.getDurationUnit());
                                ingredients.setDuration(dto.getDuration());
                                ingredients.setPower(dto.getPower());
                                ingredients.setFwdTime(dto.getFwdTime());
                                ingredients.setRevTime(dto.getRevTime());
                                ingredients.setTimeTaken(dto.getTimeTaken());
                                ingredients.setStageDuration(dto.getStageDuration());
                                return ingredients;
                            })
                            .collect(Collectors.toList()));
                    return Mono.just(result);
                });
    }


    public Flux<CompanyOrderResponse> getCompanyOrders(String orderDate, int mealId) {
        Flux<OrderMaster> orderMasters = orderMasterRepository.findByOrderDateAndMealId(orderDate, mealId)
                        .filter(orderMaster -> orderMaster.getMealSuggestionId() > 0);

        return orderMasters
                .groupBy(OrderMaster::getCompanyId)
                .flatMap(group -> {
                    Mono<List<OrderItem>> orderItemsMono = group
                            .flatMap(orderMaster ->
                                    mealSuggestionDetailsRepository
                                            .findByMealSuggestionId(orderMaster.getMealSuggestionId())
                                            .flatMap(mealSuggestionDetails -> {
                                                int itemId = mealSuggestionDetails.getItemId();
                                                BigDecimal mealCount = new BigDecimal(orderMaster.getMealCount());
                                                return grammageRepository.findByCompanyIdAndMealIdAndItemIdAndDay
                                                                (orderMaster.getCompanyId(), mealId, itemId,
                                                                        orderMaster.getDayOfWeek())
                                                        .map(grammage -> {
                                                            OrderItem orderItem = new OrderItem();
                                                            String itemName = (String) memcachedClient.get("itemNames-"+itemId);
                                                            orderItem.setItemName(itemName);
                                                            BigDecimal quantity = grammage.getGram().multiply(mealCount);
                                                            orderItem.setQuantity(quantity);
                                                            return orderItem;
                                                        });
                                            })
                            )
                            .collectList();

                    return orderItemsMono.map(orderItems -> {
                        CompanyOrderResponse response = new CompanyOrderResponse();
                        String companyName = (String) memcachedClient.get("companyNames-" + group.key());
                        response.setCompanyName(companyName);
                        response.setItems(orderItems);
                        return response;
                    });
                });
    }


    //    Todo Every 20 sec one API Call
    private RecipeGetDetailsResponse createRecipeGetDetailsResponse(RecipeMaster master,
                                                                    List<RecipeDetailsEntity> recipeDetailsList) {
        RecipeGetDetailsResponse responseDetails = new RecipeGetDetailsResponse();
        responseDetails.setMaster(master);

        Map<Integer, List<RecipeDetailsEntity>> stageGroupedMap = recipeDetailsList.stream()
                .collect(Collectors.groupingBy(RecipeDetailsEntity::getStage));

        stageGroupedMap.forEach((stageId, dataList) -> {
            RecipeDetailsEntity commonData = dataList.get(0);
            Stage stageData = new Stage();
            dataList.forEach(stageDetailData -> {
                StageDetails stageDetails = new StageDetails();
                String itemName = (String) memcachedClient.get("itemNames-"+master.getItemId());
                String uomName = (String) memcachedClient.get("uomNames-"+master.getUomId());
                stageDetails.setItemName(itemName);
                stageDetails.setItemUomName(uomName);
                stageDetails.setMediumId(stageDetailData.getMediumId());
                stageDetails.setMediumName((String) memcachedClient.get("recipeMediumNames-"+stageDetailData.getMediumId()));
                stageDetails.setIngredientId(stageDetailData.getIngredientId());
                stageDetails.setIngredientName((String) memcachedClient.get("ingredientNames-"+stageDetailData.getIngredientId()));
                int uomId = (Integer) memcachedClient.get("ingredientUomMapper-"+stageDetailData.getIngredientId());
                stageDetails.setIngredientUomName((String) memcachedClient.get("uomNames-"+uomId));
                stageDetails.setQuantity(stageDetailData.getQuantity());
                stageDetails.setCost((BigDecimal) memcachedClient.get("ingredientCost-"+stageDetailData.getIngredientId()));

                stageData.getIngredientSteps().add(stageDetails);
            });
            VesselConfiguration vesselData = new VesselConfiguration();
            vesselData.setStageEndAlertDuration(commonData.getStageEndAlertDuration());
            vesselData.setStage(commonData.getStage());
            vesselData.setProcessId(commonData.getProcessId());
            vesselData.setProcessName((String) memcachedClient.get("recipeProcessNames-"+commonData.getProcessId()));
            vesselData.setBasePv(commonData.getBasePv());
            vesselData.setBaseSv(commonData.getBaseSv());
            vesselData.setProductionSv(commonData.getProductionSv());
            vesselData.setProductPv(commonData.getProductPv());
            vesselData.setDuration(commonData.getDuration());
            vesselData.setDurationUnit(commonData.getDurationUnit());
            vesselData.setPower(commonData.getPower());
            vesselData.setFq(commonData.getFq());
            vesselData.setFwdTime(commonData.getFwdTime());
            vesselData.setRevTime(commonData.getRevTime());
            vesselData.setStartTime(commonData.getStartTime());
            vesselData.setEndTime(commonData.getEndTime());
            vesselData.setTimeTaken(commonData.getTimeTaken());
            vesselData.setStageDuration(commonData.getStageDuration());
            stageData.setVesselData(vesselData);
            responseDetails.getStages().add(stageData);
        });

        return responseDetails;
    }

    public Mono<CustomResponse<List<RecipeSearchResponse>>> getIngredientsResponse(String partialName) {
        Flux<Ingredients> ingredients = ingredientsRepository.findByIngredientNameContainsIgnoreCase(partialName);
        Status status = new Status(200, "Success");
        return ingredients.collectList()
                .map(ingredientsList -> {
                    List<RecipeSearchResponse> simpleIngredientsList = ingredientsList.stream()
                            .map(ingredient -> new RecipeSearchResponse(
                                    ingredient.getIngredientId(), ingredient.getIngredientName(),
                                    ingredient.getUomId(),
                                            (String) memcachedClient.get("uomNames-"+ingredient.getUomId()),
                                    ingredient.getCost())
                            )
                            .toList();
                    return new CustomResponse<>(status, simpleIngredientsList);
                });
    }

    public Mono<CustomResponse<List<RecipeSearchResponse>>> getProcessByPartialName(String partialName) {
        Flux<ProcessMaster> process = processMasterRepository.findByProcessNameContainsIgnoreCase(partialName);
        Status status = new Status(200, "Success");
        return process.collectList()
                .map(processList -> {
                    List<RecipeSearchResponse> simpleProcessList = processList.stream()
                            .map(processRe -> new RecipeSearchResponse(processRe.getProcessId(),
                                    processRe.getProcessName(), 0, null, null))
                            .toList();
                    return new CustomResponse<>(status, simpleProcessList);
                });
    }

    public Mono<CustomResponse<List<RecipeSearchResponse>>> getAllProcess() {
        CustomResponse<List<RecipeSearchResponse>> response = new CustomResponse<>();
        // Try to fetch the data from Memcached
        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("processEntityList");
        if (processData != null) {
            response.setStatus(new Status(200, "Success"));
            response.setData(processData);
        } else {
            // If data is not in Memcached, you can load it here or handle the case as needed.
            response.setStatus(new Status(404, "Data not found"));
        }

        return Mono.just(response);
    }

    public Mono<CustomResponse<List<RecipeSearchResponse>>> getAllMedium() {
        CustomResponse<List<RecipeSearchResponse>> response = new CustomResponse<>();
        // Try to fetch the data from Memcached
        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("mediumEntityList");
        if (processData != null) {
            response.setStatus(new Status(200, "Success"));
            response.setData(processData);
        } else {
            // If data is not in Memcached, you can load it here or handle the case as needed.
            response.setStatus(new Status(404, "Data not found"));
        }

        return Mono.just(response);
    }


    public Mono<CustomResponse<List<RecipeSearchResponse>>> getAllIngredientValue() {
        CustomResponse<List<RecipeSearchResponse>> response = new CustomResponse<>();
        // Try to fetch the data from Memcached
        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("ingredientEntityList");
        if (processData != null) {
            response.setStatus(new Status(200, "Success"));
            response.setData(processData);
        } else {
            // If data is not in Memcached, you can load it here or handle the case as needed.
            response.setStatus(new Status(404, "Data not found"));
        }

        return Mono.just(response);
    }

    public Mono<CustomResponse<List<RecipeSearchResponse>>> getAllUomValue() {
        CustomResponse<List<RecipeSearchResponse>> response = new CustomResponse<>();
        // Try to fetch the data from Memcached
        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("uomEntityList");
        if (processData != null) {
            response.setStatus(new Status(200, "Success"));
            response.setData(processData);
        } else {
            // If data is not in Memcached, you can load it here or handle the case as needed.
            response.setStatus(new Status(404, "Data not found"));
        }

        return Mono.just(response);
    }

    public Mono<CustomResponse<List<RecipeSearchResponse>>> getAllMealValue() {
        CustomResponse<List<RecipeSearchResponse>> response = new CustomResponse<>();
        // Try to fetch the data from Memcached
        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("mealEntityList");
        if (processData != null) {
            response.setStatus(new Status(200, "Success"));
            response.setData(processData);
        } else {
            // If data is not in Memcached, you can load it here or handle the case as needed.
            response.setStatus(new Status(404, "Data not found"));
        }

        return Mono.just(response);
    }

    public Mono<CustomResponse<List<RecipeSearchResponse>>> getAllCategoryValue() {
        CustomResponse<List<RecipeSearchResponse>> response = new CustomResponse<>();
        // Try to fetch the data from Memcached
        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("categoryEntityList");
        if (processData != null) {
            response.setStatus(new Status(200, "Success"));
            response.setData(processData);
        } else {
            // If data is not in Memcached, you can load it here or handle the case as needed.
            response.setStatus(new Status(404, "Data not found"));
        }

        return Mono.just(response);
    }

    public Mono<CustomResponse<List<RecipeSearchResponse>>> getAllCompanyValue() {
        CustomResponse<List<RecipeSearchResponse>> response = new CustomResponse<>();
        // Try to fetch the data from Memcached
        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("companyEntityList");
        if (processData != null) {
            response.setStatus(new Status(200, "Success"));
            response.setData(processData);
        } else {
            // If data is not in Memcached, you can load it here or handle the case as needed.
            response.setStatus(new Status(404, "Data not found"));
        }

        return Mono.just(response);
    }

    public Mono<CustomResponse<List<RecipeSearchResponse>>> getAllItemValue() {
        CustomResponse<List<RecipeSearchResponse>> response = new CustomResponse<>();
        // Try to fetch the data from Memcached
        List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("itemEntityList");
        if (processData != null) {
            response.setStatus(new Status(200, "Success"));
            response.setData(processData);
        } else {
            // If data is not in Memcached, you can load it here or handle the case as needed.
            response.setStatus(new Status(404, "Data not found"));
        }

        return Mono.just(response);
    }


    public Mono<CustomResponse<List<RecipeSearchResponse>>> getItemByPartialName(String partialName) {
        Flux<Item> item = itemRepository.findByItemNameContainsIgnoreCase(partialName);
        Status status = new Status(200, "Success");
        return item.collectList()
                .map(itemList -> {
                    List<RecipeSearchResponse> simpleItemList = itemList.stream()
                            .map(itemRe -> new RecipeSearchResponse(itemRe.getItemId(),
                                    itemRe.getItemName(), itemRe.getUomId(), (String) memcachedClient.get("uomNames-"+itemRe.getUomId()), null))
                            .toList();
                    return new CustomResponse<>(status, simpleItemList);
                });
    }


    public Mono<CustomResponse<List<RecipeSearchResponse>>> getRecipeDescriptionByPartialName(String partialName) {
        Flux<RecipeMaster> recipeFlux = recipeMasterRepository.findByRecipeDescriptionContainsIgnoreCase(partialName);
        Flux<Item> itemFlux = itemRepository.findByItemNameContainsIgnoreCase(partialName);
        Flux<Integer> itemIdsFlux = itemFlux.map(Item::getItemId);
        Flux<RecipeMaster> recipeFluxByItem = itemIdsFlux.collectList()
                .flatMapMany(recipeMasterRepository::findByItemIdIn);
        Flux<RecipeMaster> mergedFlux = recipeFlux.concatWith(recipeFluxByItem).distinct(RecipeMaster::getRecipeId);
        Status status = new Status(200, "Success");
        return mergedFlux.collectList()
                .map(recipeList -> {
                    List<RecipeSearchResponse> simpleItemList = recipeList.stream()
                            .map(recipe -> new RecipeSearchResponse(recipe.getRecipeId(),
                                    recipe.getRecipeDescription() + "-" + recipe.getPreparationQuantity(),
                                    0, null, null))
                            .toList();
                    return new CustomResponse<>(status, simpleItemList);
                });
    }

    public Mono<CustomResponse<List<RecipeSearchResponse>>> getMediumByPartialName(String partialName) {
        Flux<MediumMaster> medium = mediumMasterRepository.findByMediumNameContainsIgnoreCase(partialName);
        Status status = new Status(200, "Success");
        return medium.collectList()
                .map(mediumList -> {
                    List<RecipeSearchResponse> simpleMediumList = mediumList.stream()
                            .map(mediumRe -> new RecipeSearchResponse(mediumRe.getMediumId(),
                                    mediumRe.getMediumName(), 0, null, null))
                            .toList();
                    return new CustomResponse<>(status, simpleMediumList);
                });
    }

    public Mono<RecipeResponse> getAllRecipeWithItem(Pageable pageable) {
        Flux<RecipeWithItem> recipeWithItemFlux = recipeMasterRepository.findAllWithPagination(pageable)
                .flatMap(recipeMaster -> itemRepository.findById(recipeMaster.getItemId())
                        .map(item -> new RecipeWithItem(recipeMaster, item)));

        Mono<List<RecipeDetails>> recipeDetailsMono = recipeWithItemFlux
                .flatMap(this::toRecipeDetails)
                .collectList()
                .flatMap(this::calculatePriceForRecipeList);

        Mono<Integer> totalCountMono = getTotalRecipeCount();
        return recipeDetailsMono.flatMap(recipeDetailsList -> {
            RecipeResponse response = new RecipeResponse();
            response.setData(recipeDetailsList);
            response.setStatus(new Status(200, "Success"));
            return totalCountMono.flatMap(totalCount -> {
                PaginationInfo paginationInfo = createPaginationInfo(totalCount, pageable);
                response.setPaginationInfo(paginationInfo);
                return Mono.just(response);
            });
        });
    }


    public Mono<RecipeResponse> searchRecipesWithPagination(Pageable pageable, String partialItemName) {
        Flux<RecipeWithItem> recipeWithItemFlux = recipeMasterRepository.findByPartialItemNameWithPagination(partialItemName, pageable)
                .flatMap(recipeMaster -> itemRepository.findById(recipeMaster.getItemId())
                        .map(item -> new RecipeWithItem(recipeMaster, item)));

        Mono<List<RecipeDetails>> recipeDetailsMono = recipeWithItemFlux
                .flatMap(this::toRecipeDetails)
                .collectList()
                .flatMap(this::calculatePriceForRecipeList);

        Mono<Integer> totalCountMono = recipeMasterRepository.countByPartialItemName(partialItemName); // Count based on recipe ID

        return Mono.zip(recipeDetailsMono, totalCountMono)
                .flatMap(tuple -> {
                    List<RecipeDetails> recipeDetailsList = tuple.getT1();
                    Integer totalCount = tuple.getT2();

                    RecipeResponse response = new RecipeResponse();
                    response.setData(recipeDetailsList);
                    response.setStatus(new Status(200, "Success"));

                    PaginationInfo paginationInfo = createPaginationInfo(totalCount, pageable);
                    response.setPaginationInfo(paginationInfo);

                    return Mono.just(response);
                });
    }


    private Mono<List<RecipeDetails>> calculatePriceForRecipeList(List<RecipeDetails> recipeList) {
        return Flux.fromIterable(recipeList)
                .flatMap(recipe -> {
                    int recipeId = recipe.getRecipeId();
                    return recipeDetailsRepository.findByRecipeId(recipeId)
                            .collectList()
                            .map(detailsList -> {
                                BigDecimal totalCost = BigDecimal.ZERO;

                                for (RecipeDetailsEntity detail : detailsList) {
                                    BigDecimal ingCost = (BigDecimal) memcachedClient.get("ingredientCost-"+detail.getIngredientId());
                                    BigDecimal costOfIngredient = ingCost.multiply(detail.getQuantity());

                                    totalCost = totalCost.add(costOfIngredient);
                                }

                                recipe.setRecipeCost(totalCost);
                                return recipe;
                            });
                })
                .collectList();
    }

    public Mono<CustomResponse> createRecipeNew(RecipeMasterDetailsRequest recipeRequest) {
        boolean isDraft = "Y".equalsIgnoreCase(recipeRequest.getIsDraft());
        CustomResponse response = new CustomResponse();
        if (recipeRequest.getRecipeMasterRequest().getPreparationQuantity() != 0 && !CollectionUtils.isEmpty
                (recipeRequest.getRecipeDetailsEntityRequest())) {
            RecipeMaster recipeMasterRequest = recipeRequest.getRecipeMasterRequest();

            recipeMasterRequest.setUomId(recipeMasterRequest.getUomId());
            recipeMasterRequest.setCreatedTime(Instant.now());
            recipeMasterRequest.setActive(!isDraft);
            return recipeMasterRepository.save(recipeMasterRequest)
                    .flatMap(savedRecipeMaster -> {
                        List<RecipeStageRequest> recipeStageRequestList = recipeRequest.getRecipeDetailsEntityRequest();
                        if (CollectionUtils.isEmpty(recipeStageRequestList)) {
                            response.setStatus(new Status(500, "Recipe details are required"));
                            return Mono.just(response);
                        }
                        List<Mono<RecipeDetailsEntity>> recipeDetailsSaves = new ArrayList<>();
                        recipeStageRequestList.forEach(details -> details.getIngredientSteps().forEach(ingredirnDetails -> {
                            try {
                                System.out.println(ingredirnDetails);
//                                TODO : Recipe cost calculation - remove from Recipe service, do this in meal suggestion GET
                                RecipeDetailsEntity recipeDetailsEntity = new RecipeDetailsEntity();
                                BigDecimal ingredientCost = (BigDecimal) memcachedClient.get("ingredientCost-"+ingredirnDetails.getIngredientId());
                                System.out.println("Ingredient ID: " + ingredirnDetails.getIngredientId());
                                if (ingredientCost != null) {
                                    BigDecimal cost = ingredientCost.multiply(ingredirnDetails.getQuantity());
                                    recipeDetailsEntity.setIngredientCost(cost);
                                } else {
                                    // Handle the case where ingredientCost is null
                                    recipeDetailsEntity.setIngredientCost(BigDecimal.ZERO);
                                }
                                recipeDetailsEntity.setRecipeId(savedRecipeMaster.getRecipeId());
                                recipeDetailsEntity.setIngredientId(ingredirnDetails.getIngredientId());
                                recipeDetailsEntity.setQuantity(ingredirnDetails.getQuantity());
//                                    recipeDetailsEntity.setStage(ingredirnDetails.getStage());
//                                    recipeDetailsEntity.setProcessId(ingredirnDetails.getProcessId());
                                recipeDetailsEntity.setMediumId(ingredirnDetails.getMediumId());
                                System.out.println(details.getVesselData());
                                recipeDetailsEntity.setFq(details.getVesselData().getFq());
                                recipeDetailsEntity.setBaseSv(details.getVesselData().getBaseSv());
                                recipeDetailsEntity.setBasePv(details.getVesselData().getBasePv());
                                recipeDetailsEntity.setProductionSv(details.getVesselData().getProductionSv());
                                recipeDetailsEntity.setProductPv(details.getVesselData().getProductPv());
                                recipeDetailsEntity.setDuration(details.getVesselData().getDuration());
                                recipeDetailsEntity.setDurationUnit(details.getVesselData().getDurationUnit());
                                recipeDetailsEntity.setPower(details.getVesselData().getPower());
                                recipeDetailsEntity.setFwdTime(details.getVesselData().getFwdTime());
                                recipeDetailsEntity.setRevTime(details.getVesselData().getRevTime());
                                recipeDetailsEntity.setStartTime(details.getVesselData().getStartTime());
                                recipeDetailsEntity.setEndTime(details.getVesselData().getEndTime());
                                recipeDetailsEntity.setTimeTaken(details.getVesselData().getTimeTaken());
                                recipeDetailsEntity.setStageDuration(details.getVesselData().getStageDuration());
                                recipeDetailsEntity.setStage(details.getVesselData().getStage());
                                recipeDetailsEntity.setProcessId(details.getVesselData().getProcessId());
                                recipeDetailsEntity.setStageEndAlertDuration(details.getVesselData().getStageEndAlertDuration());

                                recipeDetailsEntity.setCreatedBy(savedRecipeMaster.getCreatedBy());
                                recipeDetailsEntity.setCreatedTime(savedRecipeMaster.getCreatedTime());
                                recipeDetailsEntity.setActive(true);
                                recipeDetailsSaves.add(recipeDetailsRepository.save(recipeDetailsEntity));
                                System.out.println("Recipe ID: " + savedRecipeMaster.getRecipeId() + "RecipeDetail Id:" + recipeDetailsEntity.getRecipeDetailId());

                            } catch (Exception e) {
                                System.err.println("Error processing ingredient details: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }));
                        return Flux.concat(recipeDetailsSaves).collectList()
                                .map(savedDetails -> {
                                    response.setStatus(new Status(200, "Recipe and details added successfully"));
                                    response.setData(recipeRequest);
                                    return response;
                                });
                    });
        } else {
            response.setStatus(new Status(500, "All fields are Required"));
            return Mono.just(response);
        }
    }

    public Mono<CustomResponse> updateRecipeMaster(int recipeId, UpdateRecipeRequest updateRecipeRequest) {
        boolean isDraft = "Y".equalsIgnoreCase(updateRecipeRequest.getIsDraft());
        RecipeMaster updatedRecipeMaster = updateRecipeRequest.getRecipeMasterRequest();
        List<RecipeStageRequest> newRecipeStageDetails = updateRecipeRequest.getRecipeDetailsEntityRequest();

        return recipeMasterRepository.findById(recipeId)
                .flatMap(existingRecipeMaster -> {
                    existingRecipeMaster.setItemId(updatedRecipeMaster.getItemId());
                    existingRecipeMaster.setUomId(updatedRecipeMaster.getUomId());
                    existingRecipeMaster.setPreparationQuantity(updatedRecipeMaster.getPreparationQuantity());
                    existingRecipeMaster.setRecipeDescription(updatedRecipeMaster.getRecipeDescription());
                    existingRecipeMaster.setTotalTimeTaken(updatedRecipeMaster.getTotalTimeTaken());
                    existingRecipeMaster.setPreparationType(updatedRecipeMaster.getPreparationType());
                    existingRecipeMaster.setActive(!isDraft);

                    Mono<Void> saveAndDelete = recipeMasterRepository.save(existingRecipeMaster)
                            .then(recipeDetailsRepository.deleteByRecipeId(recipeId));

                    Flux<Void> insertDetails = Flux.fromIterable(newRecipeStageDetails)
                            .flatMap(newStageRequest -> {
                                VesselIndentIngredientsResponse vesselData = newStageRequest.getVesselData();
                                List<IngredientStageRequest> ingredientSteps = newStageRequest.getIngredientSteps();

                                return Flux.fromIterable(ingredientSteps)
                                        .flatMap(ingredientStep -> {
                                            RecipeDetailsEntity newDetail = new RecipeDetailsEntity();

                                            newDetail.setRecipeId(recipeId);
                                            newDetail.setBaseSv(vesselData.getBaseSv());
                                            newDetail.setBasePv(vesselData.getBasePv());
                                            newDetail.setProductionSv(vesselData.getProductionSv());
                                            newDetail.setProductPv(vesselData.getProductPv());
                                            newDetail.setDuration(vesselData.getDuration());
                                            newDetail.setDurationUnit(vesselData.getDurationUnit());
                                            newDetail.setPower(vesselData.getPower());
                                            newDetail.setFq(vesselData.getFq());
                                            newDetail.setFwdTime(vesselData.getFwdTime());
                                            newDetail.setRevTime(vesselData.getRevTime());
                                            newDetail.setStartTime(vesselData.getStartTime());
                                            newDetail.setEndTime(vesselData.getEndTime());
                                            newDetail.setTimeTaken(vesselData.getTimeTaken());
                                            newDetail.setStageEndAlertDuration(vesselData.getStageEndAlertDuration());
                                            newDetail.setProcessId(vesselData.getProcessId());
                                            newDetail.setStage(vesselData.getStage());
                                            newDetail.setStageDuration(vesselData.getStageDuration());

                                            newDetail.setIngredientId(ingredientStep.getIngredientId());

                                            BigDecimal ingredientCost = (BigDecimal) memcachedClient.get("ingredientCost-"+ingredientStep.getIngredientId());
                                            if (ingredientCost != null) {
                                                BigDecimal cost = ingredientCost.multiply(ingredientStep.getQuantity());
                                                newDetail.setIngredientCost(cost);
                                            } else {
                                                // Handle the case where ingredientCost is null
                                                newDetail.setIngredientCost(BigDecimal.ZERO);
                                            }

                                            newDetail.setQuantity(ingredientStep.getQuantity());
                                            newDetail.setMediumId(ingredientStep.getMediumId());

                                            return recipeDetailsRepository.save(newDetail).then();
                                        });
                            });

                    return saveAndDelete.then(insertDetails.then(Mono.just(existingRecipeMaster)))
                            .map(this::createCustomResponses)
                            .onErrorReturn(createErrorResponses());
                });
    }

    public Mono<CustomResponse> updateRecipe(int recipeId, RecipeRequest updatedRecipeRequest) {
        return recipeMasterRepository.findById(recipeId)
                .flatMap(existingRecipe -> updateRecipeDetails(existingRecipe, updatedRecipeRequest))
                .switchIfEmpty(handleRecipeNotFound())
                .onErrorResume(throwable -> handleUpdateError());
    }

    private Mono<CustomResponse> updateRecipeDetails(RecipeMaster existingRecipe, RecipeRequest updatedRecipeRequest) {
        try {
            existingRecipe.setItemId(updatedRecipeRequest.getRecipeMasterRequest().getItemId());
            existingRecipe.setRecipeDescription(updatedRecipeRequest.getRecipeMasterRequest().getRecipeDescription());
            existingRecipe.setPreparationQuantity(updatedRecipeRequest.getRecipeMasterRequest().getPreparationQuantity());
            existingRecipe.setPreparationType(updatedRecipeRequest.getRecipeMasterRequest().getPreparationType());
            existingRecipe.setCreatedBy(updatedRecipeRequest.getRecipeMasterRequest().getCreatedBy());
            existingRecipe.setUpdatedTime(Instant.now());
            existingRecipe.setUpdatedBy(updatedRecipeRequest.getRecipeMasterRequest().getUpdatedBy());
            existingRecipe.setUpdatedTime(Instant.now());
            existingRecipe.setActive(true);
            return recipeMasterRepository.save(existingRecipe)
                    .flatMap(savedRecipeMaster -> deleteAndInsertRecipeDetails(savedRecipeMaster, updatedRecipeRequest))
                    .onErrorResume(throwable -> {
                        log.error("Error while updating recipe details: {}", throwable.getMessage());
                        return Mono.error(new RuntimeException("An error occurred during recipe update"));
                    });
        } catch (Exception e) {
            log.error("Error while updating recipe details: {}", e.getMessage());
            return Mono.error(new RuntimeException("An error occurred during recipe update"));
        }
    }

    private Mono<CustomResponse> deleteAndInsertRecipeDetails(RecipeMaster savedRecipeMaster, RecipeRequest updatedRecipeRequest) {
        return recipeDetailsRepository.deleteByRecipeId(savedRecipeMaster.getRecipeId())
                .then(updateNewRecipeDetails(savedRecipeMaster, updatedRecipeRequest))
                .onErrorResume(throwable -> handleUpdateError());
    }

    private Mono<CustomResponse> updateNewRecipeDetails(RecipeMaster savedRecipeMaster, RecipeRequest updatedRecipeRequest) {
        try {
            CustomResponse response = new CustomResponse();
            List<RecipeDetailsEntity> recipeDetailsEntityList = updatedRecipeRequest.getRecipeDetailsEntityRequest();
            if (CollectionUtils.isEmpty(recipeDetailsEntityList)) {
                response.setStatus(new Status(500, "Recipe details are required"));
                return Mono.just(response);
            } else {
                recipeDetailsEntityList.forEach(details -> {
                    BigDecimal ingredientCost = (BigDecimal) memcachedClient.get("ingredientCost-"+details.getIngredientId());
//                    BigDecimal quantity = new BigDecimal(details.getQuantity())
//                            .setScale(2, RoundingMode.HALF_EVEN); // Set scale and rounding mode;

                    if (ingredientCost != null) {
                        BigDecimal cost = ingredientCost.multiply(details.getQuantity());
                        details.setIngredientCost(cost);
                    } else {
                        // Handle the case where ingredientCost is null
                        details.setIngredientCost(BigDecimal.ZERO);
                    }
                    details.setCreatedBy(savedRecipeMaster.getCreatedBy());
                    details.setCreatedTime(savedRecipeMaster.getCreatedTime());
                    details.setUpdatedBy(savedRecipeMaster.getUpdatedBy());
                    details.setUpdatedTime(savedRecipeMaster.getUpdatedTime());
                    details.setActive(true);
                    details.setRecipeId(savedRecipeMaster.getRecipeId());
                    details.setRecipeDetailId(0);
                });
                return recipeDetailsRepository.saveAll(recipeDetailsEntityList)
                        .collectList()
                        .map(savedDetailsList -> new CustomResponse(new Status(200, "Recipe and Details Updated Successfully"), updatedRecipeRequest));
            }
        } catch (Exception e) {
            log.error("Error while updating recipe details: {}", e.getMessage());
            return Mono.error(new RuntimeException("An error occurred during recipe update"));
        }
    }

    private Mono<CustomResponse> handleRecipeNotFound() {
        CustomResponse response = new CustomResponse();
        response.setStatus(new Status(404, "Recipe Not Found"));
        return Mono.just(response);
    }

    private Mono<CustomResponse> handleUpdateError() {
        CustomResponse response = new CustomResponse();
        response.setStatus(new Status(500, "An error occurred during recipe update"));
        return Mono.just(response);
    }

    public Mono<Integer> getTotalRecipeCount() {
        return recipeMasterRepository.count().map(Long::intValue);
    }


    private Mono<RecipeDetails> toRecipeDetails(RecipeWithItem recipeWithItem) {
        RecipeMaster recipeMaster = recipeWithItem.getRecipeMaster();
        Item item = recipeWithItem.getItem();

        RecipeDetails recipeDetails = new RecipeDetails();
        recipeDetails.setCreatedBy(recipeMaster.getCreatedBy());
        recipeDetails.setCreatedTime(recipeMaster.getCreatedTime());
        recipeDetails.setUpdatedBy(recipeMaster.getUpdatedBy());
        recipeDetails.setUpdatedTime(recipeMaster.getUpdatedTime());
        recipeDetails.setRecipeId(recipeMaster.getRecipeId());
        recipeDetails.setRecipeDescription(recipeMaster.getRecipeDescription());
        recipeDetails.setTotalTimeTaken(recipeMaster.getTotalTimeTaken());
        recipeDetails.setPreparationQuantity(recipeMaster.getPreparationQuantity());
        recipeDetails.setPreparationType(recipeMaster.getPreparationType());
        recipeDetails.setActive(recipeMaster.isActive());

        ItemDetailsResponse itemDetailsResponse = new ItemDetailsResponse();
        itemDetailsResponse.setCreatedBy(item.getCreatedBy());
        itemDetailsResponse.setCreatedTime(item.getCreatedTime());
        itemDetailsResponse.setUpdatedBy(item.getUpdatedBy());
        itemDetailsResponse.setItemName(item.getItemName());
        itemDetailsResponse.setItemId(item.getItemId());
        itemDetailsResponse.setUomId(item.getUomId());
        itemDetailsResponse.setActive(item.isActive());

        return uomMasterRepository.findById(item.getUomId())
                .map(uomMaster -> {
                    itemDetailsResponse.setUomName(uomMaster != null ? uomMaster.getName() : null);
                    recipeDetails.setItemDetailsResponse(itemDetailsResponse);
                    return recipeDetails;
                });
    }


    private PaginationInfo createPaginationInfo(int totalCount, Pageable pageable) {
        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setTotalCount(totalCount);
        paginationInfo.setCurrentPage(pageable.getPageNumber() + 1);
        paginationInfo.setPageSize(pageable.getPageSize());
        return paginationInfo;
    }

    private CustomResponse createCustomResponses(RecipeMaster recipeMaster) {
        CustomResponse response = new CustomResponse();
        response.setStatus(new Status(200, "Recipes Updated Successfully"));
        response.setData(recipeMaster);
        return response;
    }

    private CustomResponse createErrorResponses() {
        CustomResponse response = new CustomResponse();
        response.setStatus(new Status(500, "Error"));
        return response;
    }

}
