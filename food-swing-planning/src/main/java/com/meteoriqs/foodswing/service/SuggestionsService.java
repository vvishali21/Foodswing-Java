package com.meteoriqs.foodswing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.meteoriqs.foodswing.data.entity.CompanyMealCostMapping;
import com.meteoriqs.foodswing.data.entity.MealProductionPlan;
import com.meteoriqs.foodswing.model.RecipeResponseGetModelPlanning;
import com.meteoriqs.foodswing.apiclient.ProductionFeignClient;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.entity.RecipeMaster;
import com.meteoriqs.foodswing.data.model.SuggestionQuantityUpdateRequest;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.data.repository.*;
import com.meteoriqs.foodswing.model.*;
import com.meteoriqs.foodswing.model.MenuSuggestionResponse.SuggestedItem;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SuggestionsService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionsService.class);
    private final MealItemMappingRepository mealItemMappingRepository;
    private final MealSuggestionDetailsRepository mealSuggestionDetailsRepository;
    private final MealSuggestionRepository mealSuggestionRepository;
    private final OrderMasterRepository orderMasterRepository;
    private final ItemRepository itemRepository;
    private final RecipeMasterRepository recipeMasterRepository;
    private final RecipeDetailsRepository recipeDetailsRepository;
    private final SequenceMasterRepository sequenceMasterRepository;
    private final MealRepository mealRepository;
    private final CompanyRepository companyRepository;
    private final UomMasterRepository uomMasterRepository;
    private final IngredientsRepository ingredientsRepository;
    private final ProductionFeignClient productionApiClient;
    private final MealCategoryMappingRepository mealCategoryMappingRepository;
    private final CompanyCostMappingRepository companyCostMappingRepository;
    private final MemcachedClient memcachedClient;
    private final VesselItemMappingRepository vesselItemMappingRepository;
    private final VesselRepository vesselRepository;
    private final ValidatePlanIndentRepository validatePlanIndentRepository;
    private final GrammageRepository grammageRepository;
    private final MealProductionPlanRepository mealProductionPlanRepository;

    @Autowired
    public SuggestionsService(MealItemMappingRepository mealItemMappingRepository,
                              MealSuggestionDetailsRepository mealSuggestionDetailsRepository,
                              MealSuggestionRepository mealSuggestionRepository, OrderMasterRepository orderMasterRepository,
                              RecipeMasterRepository recipeMasterRepository, ProductionFeignClient productionApiClient,
                              RecipeDetailsRepository recipeDetailsRepository, SequenceMasterRepository sequenceMasterRepository,
                              ItemRepository itemRepository, MealRepository mealRepository, CompanyRepository companyRepository,
                              MealCategoryMappingRepository mealCategoryMappingRepository1, CompanyCostMappingRepository companyCostMappingRepository,
                              MemcachedClient memcachedClient, UomMasterRepository uomMasterRepository,
                              IngredientsRepository ingredientsRepository, MealCategoryMappingRepository mealCategoryMappingRepository,
                              VesselItemMappingRepository vesselItemMappingRepository, VesselRepository vesselRepository, ValidatePlanIndentRepository validatePlanIndentRepository,
                              GrammageRepository grammageRepository, MealProductionPlanRepository mealProductionPlanRepository) {
        this.mealItemMappingRepository = mealItemMappingRepository;
        this.mealSuggestionDetailsRepository = mealSuggestionDetailsRepository;
        this.mealSuggestionRepository = mealSuggestionRepository;
        this.orderMasterRepository = orderMasterRepository;
        this.recipeMasterRepository = recipeMasterRepository;
        this.productionApiClient = productionApiClient;
        this.recipeDetailsRepository = recipeDetailsRepository;
        this.sequenceMasterRepository = sequenceMasterRepository;
        this.itemRepository = itemRepository;
        this.mealRepository = mealRepository;
        this.companyRepository = companyRepository;
        this.uomMasterRepository = uomMasterRepository;
        this.ingredientsRepository = ingredientsRepository;
        this.mealCategoryMappingRepository = mealCategoryMappingRepository1;
        this.companyCostMappingRepository = companyCostMappingRepository;
        this.memcachedClient = memcachedClient;
        this.vesselItemMappingRepository = vesselItemMappingRepository;
        this.vesselRepository = vesselRepository;
        this.validatePlanIndentRepository = validatePlanIndentRepository;
        this.grammageRepository = grammageRepository;
        this.mealProductionPlanRepository = mealProductionPlanRepository;
    }


    public Mono<ResponseEntity<CustomResponse<Void>>> updateMealSuggestionForPlanUpdate(SuggestionQuantityUpdateRequest request) {
        int dayOfWeek = request.getDayOfWeek();
        int mealId = request.getMealId();

        return Flux.fromIterable(request.getCompanyDataList())
                .flatMap(company -> {
                    int companyId = company.getCompanyId();
                    int mealCount = company.getMealCount();

                    return grammageRepository.findByCompanyIdAndMealIdAndDay(companyId, mealId, dayOfWeek)
                            .flatMap(grammage -> {
                                int itemId = grammage.getItemId();
                                BigDecimal newQuantity = grammage.getGram().multiply(BigDecimal.valueOf(mealCount));
                                return mealSuggestionDetailsRepository.findByMealSuggestionIdAndItemId(request.getMealSuggestionId(), itemId)
                                        .flatMap(detail -> {
                                            detail.setQuantity(newQuantity);
                                            // Call a method to get recipe for the updated quantity and update recipe ID
//                                             e.g., detail.setRecipeId(getRecipeForItem(itemId, newQuantity));
                                            return getRecipeForItem(itemId, newQuantity)
                                                    .flatMap(recipeId -> {
                                                        detail.setRecipeId(recipeId);
                                                        return mealSuggestionDetailsRepository.save(detail);
                                                    });
                                        });
                            });
                })
                .then(
                        Mono.just(ResponseEntity.status(HttpStatus.OK)
                                .body(new CustomResponse<>(new Status(200, "Meal suggestion updated successfully."), null)))
                );
    }


    public Flux<OrderMaster> getOrders(int mealId, String orderDate) {
        return orderMasterRepository.findByMealIdAndOrderDate(mealId, orderDate)
                .flatMap(orderMaster -> {
                    Mono<Meal> mealMono = mealRepository.findById(orderMaster.getMealId());
                    Mono<Company> companyMono = companyRepository.findById(orderMaster.getCompanyId());

                    return Mono.zip(mealMono, companyMono, (meal, company) -> {
                        orderMaster.setMealName(meal.getName());
                        orderMaster.setCompanyName(company.getCompanyName());
                        return orderMaster;
                    });
                })
                .filter(orderMaster -> orderMaster.getMealSuggestionId() == 0)
                .sort(Comparator.comparingInt(OrderMaster::getOrderId));
    }

    public Flux<OrderMaster> getOrdersByMealSuggestionId(int mealId, String orderDate, int mealSuggestionId) {
        return orderMasterRepository.findByMealIdAndOrderDateAndMealSuggestionId(mealId, orderDate, mealSuggestionId)
                .flatMap(orderMaster -> {
                    Mono<Meal> mealMono = mealRepository.findById(orderMaster.getMealId());
                    Mono<Company> companyMono = companyRepository.findById(orderMaster.getCompanyId());

                    return Mono.zip(mealMono, companyMono, (meal, company) -> {
                        orderMaster.setMealName(meal.getName());
                        orderMaster.setCompanyName(company.getCompanyName());
                        return orderMaster;
                    });
                })
                .sort(Comparator.comparingInt(OrderMaster::getOrderId));
    }


    public Mono<CustomResponse<String>> processPlans(PlanUpdateandDeletePayload payload) {
        // First, update meal suggestions
        return updateMealSuggestionForPlanUpdate(payload.getMealSuggestionPlanUpdate())
                .flatMap(response -> {
                    // After updating meal suggestions, process the plans
                    if (!payload.getDeleted().isEmpty()) {
                        for (Integer orderId : payload.getDeleted()) {
                            orderMasterRepository.findById(orderId)
                                    .doOnSuccess(existingOrder -> {
                                        existingOrder.setMealSuggestionId(0);
                                        orderMasterRepository.save(existingOrder).subscribe();
                                    })
                                    .subscribe();
                        }
                    }

                    for (OrderMaster order : payload.getOrders()) {
                        orderMasterRepository.findById(order.getOrderId())
                                .doOnSuccess(existingOrder -> {
                                    existingOrder.setMealCount(order.getMealCount());
                                    orderMasterRepository.save(existingOrder).subscribe();
                                })
                                .subscribe();
                    }

                    return Mono.just(new CustomResponse<>(new Status(200, "Plans updated successfully"), "Plans updated successfully."));
                });
    }


    public Flux<PlanList> getPlanList(int mealId, String orderDate) {
        return orderMasterRepository.findByOrderDateAndMealId(mealId, orderDate);
    }


    public Mono<String> completePlan(int planId, int mealSuggestionId) {
        return mealProductionPlanRepository.findByPlanId(planId)
                .collectList()
                .flatMap(plans -> {
                    List<Integer> vesselIds = plans.stream()
                            .map(MealProductionPlan::getVesselId)
                            .collect(Collectors.toList());

                    return vesselRepository.findByVesselIdIn(vesselIds)
                            .flatMap(vessel -> {
                                vessel.setStatus(0);
                                return vesselRepository.save(vessel);
                            })
                            .collectList()
                            .flatMap(vesselList -> orderMasterRepository.updateStatusByMealSuggestionId(3, mealSuggestionId)
                                    .thenReturn("Success"))
                            .doOnError(error -> {
                                log.error("Error in completePlan: {}", error.getMessage());
                            });
                })
                .onErrorResume(Mono::error);
    }



    public Mono<Void> savePlanning(int mealSuggestionId, List<Integer> orderIds) {
        return orderMasterRepository.findAllById(orderIds)
                .flatMap(order -> {
                    order.setMealSuggestionId(mealSuggestionId);
                    return orderMasterRepository.save(order);
                })
                .then();
    }



    public Mono<Map<String, List<Map<String, Object>>>> getValidatePlanVessel(int mealSuggestionId) {
        Flux<MealSuggestionDetails> mealSuggestionDetailsFlux =
                mealSuggestionDetailsRepository.findByMealSuggestionId(mealSuggestionId);

        Set<Integer> globalUsedVesselIds = new HashSet<>(); // Track used vessel IDs globally

        return mealSuggestionDetailsFlux
                .flatMap(mealSuggestionDetail -> {
                    int itemId = mealSuggestionDetail.getItemId();
                    double quantityNeeded = mealSuggestionDetail.getQuantity().doubleValue();

                    String itemName = (String) memcachedClient.get("itemNames-" + itemId);

                    // Find available vessels with status 0
                    Flux<VesselItemMapping> vesselItemMappingFlux = vesselItemMappingRepository.findByItemId(itemId);
                    Flux<Vessel> availableVessels = vesselItemMappingFlux
                            .flatMap(vesselMapping -> vesselRepository.findByVesselIdAndStatus(vesselMapping.getVesselId(), 0));

                    // Retrieve planId from MealSuggestion based on mealSuggestionId
                    Mono<Integer> planIdMono = mealSuggestionRepository.getPlanIdByMealSuggestionId(mealSuggestionId);

                    return Mono.zip(planIdMono, availableVessels.collectSortedList(Comparator.comparing(Vessel::getCapacity).reversed()), (planId, sortedList) -> {

                        List<Map<String, Object>> selectedVessels = new ArrayList<>();
                        double remainingQuantity = quantityNeeded;

                        for (Vessel vessel : sortedList) {
                            int vesselId = vessel.getVesselId();
                            BigDecimal maxCapacity = vessel.getCapacity();
                            double maxCapacityDouble = maxCapacity.doubleValue();
                            String vesselName = vessel.getVesselName(); // Get the vessel name

                            if (remainingQuantity <= 0 || globalUsedVesselIds.contains(vesselId)) {
                                // Skip this vessel if it's already used or not needed
                                continue;
                            }

                            Map<String, Object> vesselInfo = new HashMap<>();
                            vesselInfo.put("vesselId", vesselId);
                            vesselInfo.put("vesselName", vesselName);
                            vesselInfo.put("maxCapacity", maxCapacity);

                            // Include the item name in the vessel info
                            vesselInfo.put("itemId", itemId);
                            vesselInfo.put("itemName", itemName);

                            if (remainingQuantity >= maxCapacityDouble) {
                                // Create a recipe for the max capacity of this vessel
                                getRecipeForItem(itemId, maxCapacity)
                                        .map(recipeId -> {
                                            Map<String, Object> recipeInfo = new HashMap<>();
                                            recipeInfo.put("RecipeId created for maxCapacity", recipeId);
                                            selectedVessels.add(recipeInfo);

                                            // Save to MealProductionPlan table
                                            MealProductionPlan mealProductionPlan = new MealProductionPlan();
                                            mealProductionPlan.setPlanId(planId);
                                            mealProductionPlan.setMealSuggestionId(mealSuggestionId);
                                            mealProductionPlan.setItemId(itemId);
                                            mealProductionPlan.setVesselId(vesselId);
                                            mealProductionPlan.setRecipeId(recipeId);
                                            mealProductionPlanRepository.save(mealProductionPlan).subscribe();

                                            return recipeId;
                                        })
                                        .subscribe();
                                globalUsedVesselIds.add(vesselId); // Mark the vessel as used globally
                                remainingQuantity -= maxCapacityDouble;
                            } else {
                                // Create a recipe for the remaining quantity
                                getRecipeForItem(itemId, BigDecimal.valueOf(remainingQuantity))
                                        .map(recipeId -> {
                                            Map<String, Object> recipeInfo = new HashMap<>();
                                            recipeInfo.put("RecipeId created for remainingQuantity", recipeId);
                                            selectedVessels.add(recipeInfo);

                                            // Save to MealProductionPlan table
                                            MealProductionPlan mealProductionPlan = new MealProductionPlan();
                                            mealProductionPlan.setPlanId(planId);
                                            mealProductionPlan.setMealSuggestionId(mealSuggestionId);
                                            mealProductionPlan.setItemId(itemId);
                                            mealProductionPlan.setVesselId(vesselId);
                                            mealProductionPlan.setRecipeId(recipeId);
                                            mealProductionPlanRepository.save(mealProductionPlan).subscribe();

                                            return recipeId;
                                        })
                                        .subscribe();

                                remainingQuantity = 0;
                            }
                            // Include the "neededMaxCapacity" field in the same vesselInfo map
//                            vesselInfo.put("neededMaxCapacity", remainingQuantity);
                            selectedVessels.add(vesselInfo);
                            globalUsedVesselIds.add(vesselId);
                        }


                        // Create a data object for this item and add it to the list
                        Map<String, List<Map<String, Object>>> itemCapacityMap = new HashMap<>();
                        itemCapacityMap.put("itemId: " + itemId, selectedVessels);

                        return Mono.just(itemCapacityMap);
                    });
                })
                .flatMap(itemCapacityMap -> itemCapacityMap)
                .collectList()
                .map(dataList -> {
                    Map<String, List<Map<String, Object>>> finalDataMap = new HashMap<>();
                    dataList.forEach(finalDataMap::putAll);
                    return finalDataMap;
                });
    }


    public Flux<ValidatePlanIndentResponse> getValidatePlanIngredients(int mealSuggestionId) {
        return validatePlanIndentRepository.getValidatePlanIndent(mealSuggestionId);
    }

    public Mono<CombinedResponse> combineResponses(int mealSuggestionId) {
        Mono<Map<String, List<Map<String, Object>>>> vesselPlanningMono = getValidatePlanVessel(mealSuggestionId);

        Flux<ValidatePlanIndentResponse> ingredientsFlux = getValidatePlanIngredients(mealSuggestionId);

        return Mono.zip(vesselPlanningMono, ingredientsFlux.collectList(), (vesselPlanningData, ingredients) -> {
            Status status = new Status(200, "Success");
            VesselPlanningResponse vesselPlanning = new VesselPlanningResponse(vesselPlanningData);

            // Create a list for items with no available vessels
            List<String> noVesselAvailableForItem = new ArrayList<>();

            // Iterate through vesselPlanningData to find items with no vessels
            vesselPlanningData.forEach((key, value) -> {
                if (value.isEmpty()) {
                    int itemId = Integer.parseInt(key.split(": ")[1]);
                    String itemName = (String) memcachedClient.get("itemNames-" + itemId);
                    if (itemName != null) {
                        noVesselAvailableForItem.add(itemName);
                    }
                }
            });

            // Create the final CombinedResponse with the custom JSON structure
            CombinedResponse combinedResponse = new CombinedResponse(status, vesselPlanning, new IngredientsResponse(ingredients));
            combinedResponse.setNoVesselAvailableForItem(noVesselAvailableForItem);

            return combinedResponse;
        });
    }



    public Flux<Void> approvePlanning(VesselPlanRequest payload) {
        int mealSuggestionId = payload.getMealSuggestionId();
        List<Integer> vesselIds = payload.getVesselIds();

        return orderMasterRepository.updateOrderStatus(mealSuggestionId)
                .thenMany(vesselRepository.updateVesselStatus(vesselIds));
    }


    public Mono<CustomResponse<MealSuggestionResponse>> getMealSuggestions(int mealId, String orderDate,
                                                                           String companyIds,
                                                                           int mealCount) {
        List<Integer> companyIdList = Arrays.stream(companyIds.split(","))
                .map(Integer::parseInt)
                .toList();

        Mono<Integer> totalCount = getTotalCountForMealId(mealId);

        return getAllPossibleMealCombos(mealId)
                .flatMap(suggestedMenuList ->
                        saveMealSuggestions(suggestedMenuList, orderDate, mealId, companyIdList, mealCount)
                                .zipWith(totalCount, (savedSuggestions, count) -> {
                                    Set<List<MenuSuggestionResponse.SuggestedItem>> uniqueCombinations = new HashSet<>();
                                    List<MenuSuggestionResponse> filteredMenuSuggestions = savedSuggestions.getData().getMenuSuggestion()
                                            .stream()
                                            .filter(menuSuggestion -> {
                                                // Sort the mealSuggestionDetails by itemId
                                                List<MenuSuggestionResponse.SuggestedItem> sortedItems = menuSuggestion.getMealSuggestionDetails()
                                                        .stream()
                                                        .sorted(Comparator.comparingInt(MenuSuggestionResponse.SuggestedItem::getItemId))
                                                        .collect(Collectors.toList());

                                                if (sortedItems.size() == count) {
                                                    boolean isUnique = uniqueCombinations.add(sortedItems);
                                                    return isUnique;
                                                }
                                                return false;
                                            })
                                            .map(menuSuggestion -> new MenuSuggestionResponse(
                                                    menuSuggestion.getMealSuggestionId(),
                                                    menuSuggestion.getTotalMakingCost(),
                                                    menuSuggestion.getCostPerMeal(),
                                                    menuSuggestion.getPercentOfSaleCost(),
                                                    menuSuggestion.getMealSuggestionDetails()
                                            ))
                                            .collect(Collectors.toList());

                                    MealSuggestionResponse mealSuggestionResponse = new MealSuggestionResponse(mealId,
                                            savedSuggestions.getData().getItemsWithoutRecipe(), filteredMenuSuggestions);

                                    return new CustomResponse<>(new Status(200, "Success"), mealSuggestionResponse);
                                })
                )
                .switchIfEmpty(Mono.just(new CustomResponse<>(new Status(404, "MealSuggestion not found"), null)));
    }


    public Mono<Integer> getTotalCountForMealId(int mealId) {
        return mealCategoryMappingRepository.getTotalCountByMealId(mealId);
    }


    private Mono<List<MenuSuggestionResponse>> getAllPossibleMealCombos(int mealId) {
        // Fetch the list of category IDs dynamically for the given mealId
        return mealCategoryMappingRepository.findByMealId(mealId)
                .collectList()
                .flatMap(mealCategoryMappings -> {
                    // Extract the category IDs and counts from the fetched mappings
                    Map<Integer, Integer> categoryCounts = mealCategoryMappings.stream()
                            .collect(Collectors.toMap(MealCategoryMapping::getCategoryId, MealCategoryMapping::getCount));

                    List<Integer> categoryIds = mealCategoryMappings.stream()
                            .map(MealCategoryMapping::getCategoryId)
                            .toList();

                    // Use the dynamic category IDs in your SQL query
                    return mealItemMappingRepository.findByMealIdAndCategoryIdIn(mealId, categoryIds)
                            .collectList()
                            .map(mealItemMappingList -> {
                                Map<Integer, List<MealItemMapping>> categoryItemsMapper = mealItemMappingList.stream()
                                        .collect(Collectors.groupingBy(MealItemMapping::getCategoryId, HashMap::new,
                                                Collectors.toList()));

                                return getAllPossibleMealCombos(categoryItemsMapper, categoryCounts);
                            });
                });
    }


    private Mono<Map<Integer, Integer>> chooseRecipeForItems(Map<Integer, BigDecimal> itemQuantityMapper) {
        return Flux.fromIterable(itemQuantityMapper.entrySet())
                .flatMap(entry -> {
                    int itemId = entry.getKey();
                    BigDecimal quantity = entry.getValue();

                    log.info("Processing itemId: {}, quantity: {}", itemId, quantity);

                    return getRecipeForItem(itemId, quantity)
                            .doOnNext(recipeId ->
                                    log.info("Recipe found for itemId {} : {}", itemId, recipeId))
                            .map(recipeId -> Map.entry(itemId, recipeId));
                })
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    public Mono<RecipeMatchResult> findRecipeIdWithPlannedQuantity(Flux<RecipeMaster> recipeMasterFlux,
                                                                   BigDecimal plannedQuantity) {
        return recipeMasterFlux
                .collectList()
                .map(sortedList -> {
                    sortedList.sort(Comparator.comparing(RecipeMaster::getPreparationQuantity)); // Sort the list
                    RecipeMatchResult result = new RecipeMatchResult();

                    RecipeMaster closestBelow = null;
                    RecipeMaster closestAbove = null;

                    for (RecipeMaster recipe : sortedList) {
                        BigDecimal quantity = BigDecimal.valueOf(recipe.getPreparationQuantity());
                        if (quantity.compareTo(plannedQuantity) == 0) {
                            result.addMatchedId(recipe.getRecipeId());
                            return result; // Exact match found, no need to continue
                        } else if (quantity.compareTo(plannedQuantity) < 0) {
                            closestBelow = recipe;
                        } else {
                            if (closestAbove == null) {
                                closestAbove = recipe;
                            }
                        }
                    }

                    if (closestBelow != null) {
                        result.addClosestBelowId(closestBelow.getRecipeId());
                    }

                    if (closestAbove != null) {
                        result.addClosestAboveId(closestAbove.getRecipeId());
                    }

                    return result;
                });
    }

    private Mono<Integer> getRecipeForItem(int itemId, BigDecimal quantityNeeded) {
        Flux<RecipeMaster> recipeMasterFlux = recipeMasterRepository.findByItemId(itemId);
        return findRecipeIdWithPlannedQuantity(recipeMasterFlux, quantityNeeded)
                .map(result -> {
                    log.info("itemId : {} :: plannedQuantity : {}", itemId, quantityNeeded);
                    log.info("matched : {}", result.isPerfectMatch());
                    log.info("matchedIds : {}", result.getMatchedIds());
                    log.info("aboveIds : {}", result.getClosestAboveIds());
                    log.info("belowIds : {}", result.getClosestBelowIds());
                    int recipeId = 0;
                    if (result.isPerfectMatch()) {
                        if (!result.getMatchedIds().isEmpty()) {
                            recipeId = result.getMatchedIds().get(0);
                        } else {
                            // Handle the case when matchedIds is empty
                            // You can throw an exception or return a default value
                        }
                    } else if (!result.getClosestAboveIds().isEmpty() && !result.getClosestBelowIds().isEmpty()) {
                        // Create a new recipe by averaging these 2
                        recipeId = duplicateRecipe("AVG", result.getClosestBelowIds().get(0),
                                result.getClosestAboveIds().get(0), quantityNeeded);
                    } else if (result.getClosestAboveIds().isEmpty() && !result.getClosestBelowIds().isEmpty()) {
                        // Create a new recipe by taking value in below - and pro-rate upwards
                        recipeId = duplicateRecipe("INC", result.getClosestBelowIds().get(0), 0,
                                quantityNeeded);
                    } else if (!result.getClosestAboveIds().isEmpty() && result.getClosestBelowIds().isEmpty()) {
                        // Create a new recipe by taking value in above - and pro-rate downwards
                        recipeId = duplicateRecipe("DEC", 0, result.getClosestAboveIds().get(0),
                                quantityNeeded);
                    } else {
                        // Handle other cases or throw an exception if needed
                    }
                    log.info("identified recipeId : {}", recipeId);
                    return recipeId; // Return the calculated recipeId
                });
    }


    private int duplicateRecipe(String createType, int lowerRecipeId, int higherRecipeId, BigDecimal quantityNeeded) {
        int newRecipeId = 0;
        RecipeResponseGetModelPlanning lowerQuantityRecipe = null;
        RecipeResponseGetModelPlanning higherQuantityRecipe = null;
        RecipeMasterDetailsRequest createRecipeRequest = null;

        if (lowerRecipeId > 0) {
            lowerQuantityRecipe = getRecipeDetails(lowerRecipeId);
        }
        if (higherRecipeId > 0) {
            higherQuantityRecipe = getRecipeDetails(higherRecipeId);
        }

        switch (createType) {
            case "AVG":
                if (lowerQuantityRecipe != null && higherQuantityRecipe != null) {
                    Map<String, BigDecimal> lowQuantityMap = getItemQuantitiesInRecipe(lowerQuantityRecipe);
                    Map<String, BigDecimal> highQuantityMap = getItemQuantitiesInRecipe(higherQuantityRecipe);

                    log.info("lowQuantityMap :: {}", lowQuantityMap);
                    log.info("highQuantityMap :: {}", highQuantityMap);

                    createRecipeRequest = createSaveRequestModelFromGetResponseModel(lowerQuantityRecipe, quantityNeeded);

                    createRecipeRequest.getRecipeDetailsEntityRequest().forEach(detailData ->
                            detailData.getIngredientSteps().forEach(ingredient -> {
                                String quantityKey = detailData.getVesselData().getStage() + "-" + ingredient.getIngredientId();
                                BigDecimal lowQuantity = lowQuantityMap.getOrDefault(quantityKey, BigDecimal.ZERO);
                                BigDecimal highQuantity = highQuantityMap.getOrDefault(quantityKey, BigDecimal.ZERO);

                                BigDecimal newQuantity = (lowQuantity.add(highQuantity))
                                        .divide(new BigDecimal(2), 2, RoundingMode.HALF_UP);
                                ingredient.setQuantity(newQuantity);
                            }));
                }
                break;

            case "INC":
                if (lowerQuantityRecipe != null) {
                    Map<String, BigDecimal> lowQuantityMapInc = getItemQuantitiesInRecipe(lowerQuantityRecipe);
                    log.info("lowQuantityMapInc :: {}", lowQuantityMapInc);

                    createRecipeRequest = createSaveRequestModelFromGetResponseModel(lowerQuantityRecipe, quantityNeeded);

                    RecipeResponseGetModelPlanning finalLowerQuantityRecipe = lowerQuantityRecipe;

                    createRecipeRequest.getRecipeDetailsEntityRequest().forEach(detailData ->
                            detailData.getIngredientSteps().forEach(ingredient -> {
                                String quantityKey = detailData.getVesselData().getStage() + "-" + ingredient.getIngredientId();
                                BigDecimal ingredientQuantity = lowQuantityMapInc.getOrDefault(quantityKey, BigDecimal.ZERO);
                                BigDecimal oldRecipeQuantity = BigDecimal.valueOf(finalLowerQuantityRecipe.getData().getMaster().getPreparationQuantity());
                                BigDecimal ingredientQuantityPerHead = ingredientQuantity.divide(oldRecipeQuantity, 2, RoundingMode.HALF_UP);
                                BigDecimal newQuantity = ingredientQuantityPerHead.multiply(quantityNeeded);
                                ingredient.setQuantity(newQuantity);
                            }));
                }
                break;

            case "DEC":
                if (higherQuantityRecipe != null) {
                    Map<String, BigDecimal> lowQuantityMapDec = getItemQuantitiesInRecipe(higherQuantityRecipe);
                    log.info("lowQuantityMapDec :: {}", lowQuantityMapDec);

                    createRecipeRequest = createSaveRequestModelFromGetResponseModel(higherQuantityRecipe, quantityNeeded);

                    RecipeResponseGetModelPlanning finalHigherQuantityRecipe = higherQuantityRecipe;

                    createRecipeRequest.getRecipeDetailsEntityRequest().forEach(detailData ->
                            detailData.getIngredientSteps().forEach(ingredient -> {
                                String quantityKey = detailData.getVesselData().getStage() + "-" + ingredient.getIngredientId();
                                BigDecimal ingredientQuantity = lowQuantityMapDec.getOrDefault(quantityKey, BigDecimal.ZERO);
                                BigDecimal oldRecipeQuantity = BigDecimal.valueOf(finalHigherQuantityRecipe.getData().getMaster().getPreparationQuantity());
                                BigDecimal ingredientQuantityPerHead = ingredientQuantity.divide(oldRecipeQuantity, 2, RoundingMode.HALF_UP);
                                BigDecimal newQuantity = ingredientQuantityPerHead.multiply(quantityNeeded);
                                ingredient.setQuantity(newQuantity);
                            }));
                }
                break;

            default:
                log.error("duplicateRecipe :: default case in Switch statement");
                break;
        }

        if (createRecipeRequest != null) {
            newRecipeId = saveNewRecipe(createRecipeRequest);
        }

        return newRecipeId;
    }

    private Map<String, BigDecimal> getItemQuantitiesInRecipe(RecipeResponseGetModelPlanning recipeData) {
        Map<String, BigDecimal> ingredientQuantityMap = new HashMap<>();
        recipeData.getData().getStages().forEach(data -> data.getIngredientSteps().forEach(ingredient ->
                ingredientQuantityMap.put(data.getVesselData().getStage() + "-" + ingredient.getIngredientId(),
                        ingredient.getQuantity())));
        return ingredientQuantityMap;
    }

    private RecipeMasterDetailsRequest createSaveRequestModelFromGetResponseModel(RecipeResponseGetModelPlanning getResponseModel,
                                                                                  BigDecimal quantityNeeded) {
        RecipeMasterDetailsRequest createRecipeRequest = new RecipeMasterDetailsRequest();
        RecipeMaster masterData = new RecipeMaster();
        masterData.setItemId(getResponseModel.getData().getMaster().getItemId());//TODO Change quantity int to BigDecimal in Recipe
        masterData.setPreparationQuantity(quantityNeeded.intValue());
        masterData.setRecipeDescription(getResponseModel.getData().getMaster().getRecipeDescription());
        masterData.setUomId(getResponseModel.getData().getMaster().getUomId());
        masterData.setCreatedBy(getResponseModel.getData().getMaster().getCreatedBy());
        masterData.setTotalTimeTaken(getResponseModel.getData().getMaster().getTotalTimeTaken());   // TODO: Calculate
        masterData.setPreparationType(getResponseModel.getData().getMaster().getPreparationType());

        createRecipeRequest.setRecipeMasterRequest(masterData);
        getResponseModel.getData().getStages().forEach(stage -> {
            RecipeStageRequest stageData = new RecipeStageRequest();
            stageData.getVesselData().setBasePv(stage.getVesselData().getBasePv());
            stageData.getVesselData().setBaseSv(stage.getVesselData().getBaseSv());
            stageData.getVesselData().setDuration(stage.getVesselData().getDuration());
            stageData.getVesselData().setDurationUnit(stage.getVesselData().getDurationUnit());
            stageData.getVesselData().setEndTime(stage.getVesselData().getEndTime());
            stageData.getVesselData().setFq(stage.getVesselData().getFq());
            stageData.getVesselData().setFwdTime(stage.getVesselData().getFwdTime());

            stageData.getVesselData().setPower(stage.getVesselData().getPower());
            stageData.getVesselData().setProcessId(stage.getVesselData().getProcessId());
            stageData.getVesselData().setProductPv(stage.getVesselData().getProductPv());
            stageData.getVesselData().setProductionSv(stage.getVesselData().getProductionSv());

            stageData.getVesselData().setRevTime(stage.getVesselData().getRevTime());
            stageData.getVesselData().setStage(stage.getVesselData().getStage());
            stageData.getVesselData().setStageEndAlertDuration(stage.getVesselData().getStageEndAlertDuration());
            stageData.getVesselData().setStageDuration(stage.getVesselData().getStageDuration());
            stageData.getVesselData().setStartTime(stage.getVesselData().getStartTime());
            stageData.getVesselData().setTimeTaken(stage.getVesselData().getTimeTaken());
            stageData.getVesselData().setCreatedBy(masterData.getCreatedBy());
            stage.getIngredientSteps().forEach(ingredient -> {
                IngredientStageRequest ingredientStage = new IngredientStageRequest();
                ingredientStage.setIngredientId(ingredient.getIngredientId());
                ingredientStage.setMediumId(ingredient.getMediumId());
                ingredientStage.setQuantity(ingredient.getQuantity());

                stageData.getIngredientSteps().add(ingredientStage);
            });
            createRecipeRequest.getRecipeDetailsEntityRequest().add(stageData);
        });

        return createRecipeRequest;
    }

    private int saveNewRecipe(RecipeMasterDetailsRequest requestPayload) {
        try {
            int itemId = requestPayload.getRecipeMasterRequest().getItemId();
            BigDecimal quantity = BigDecimal.valueOf(requestPayload.getRecipeMasterRequest().getPreparationQuantity());
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String jsonPayload = mapper.writeValueAsString(requestPayload);
            log.info("jsonPayload for saveRecipe :: {}", jsonPayload);
            String createResponseJson = productionApiClient.createRecipe(jsonPayload);
            log.info("Response from productionApiClient :: {}", createResponseJson);

            // Parse the response
            SaveRecipePlanningResponse response = mapper.readValue(createResponseJson, SaveRecipePlanningResponse.class);
            log.info("Parsed response: {}", response);
            if (response != null && response.getData() != null && response.getData().getRecipeMasterRequest() != null) {
                int recipeId = response.getData().getRecipeMasterRequest().getRecipeId();
                log.info("New recipe created for item : {} and quantity : {} with recipeId :: {}",
                        itemId, quantity, recipeId);
                return recipeId;
            } else {
                log.error("Invalid or null response from the API.");
            }
        } catch (Exception e) {
            log.error("Error saving new Recipe :: {}", e.getMessage(), e);
            e.printStackTrace();
        }
        return 0;
    }

    private RecipeResponseGetModelPlanning getRecipeDetails(int recipeId) {
        String recipeJson = productionApiClient.getRecipe((long) recipeId);
        log.info("get recipe response for itemId :: {} :: {}", recipeId, recipeJson);
        log.info("recipe GET Json Response :: {}", recipeJson);
        RecipeResponseGetModelPlanning response = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            response = mapper.readValue(recipeJson, RecipeResponseGetModelPlanning.class);
        } catch (Exception e) {
            log.error("Mapper conversion Error :: {}", e.getMessage());
            e.printStackTrace();
        }
        return response;
    }


    public Mono<CustomResponse<List<MealSuggestionDetails>>> editMealSuggestion(MenuSuggestionResponse payload, int mealId, String orderDate, List<Integer> companyIdList, int mealCount) {
        payload.setMealSuggestionId(0);

        return saveMealSuggestions(Collections.singletonList(payload), orderDate, mealId, companyIdList, mealCount)
                .flatMap(result -> {
                    if (result.getData() != null) {
                        String logMessage = result.getData().toString();
                        log.info("Meal suggestion edited: {}", logMessage);

                        // Use a regular expression to extract mealSuggestionId
                        Pattern pattern = Pattern.compile("mealSuggestionId=(\\d+)");
                        Matcher matcher = pattern.matcher(logMessage);

                        if (matcher.find()) {
                            String mealSuggestionId = matcher.group(1);
                            System.out.println("mealSuggestionId: " + mealSuggestionId);

                            // Now return the result of getMealSuggestionDetailsBySuggestionId
                            return getMealSuggestionDetailsBySuggestionId(Integer.parseInt(mealSuggestionId))
                                    .collectList()
                                    .map(list -> new CustomResponse<>(new Status(200, "Meal suggestion edited successfully"), list));
                        } else {
                            System.err.println("Failed to extract mealSuggestionId from log message.");
                        }
                    } else {
                        System.err.println("No data in CustomResponse.");
                    }

                    // If extraction or other conditions failed, return an empty Mono
                    return Mono.just(new CustomResponse<>(new Status(400, "Error"), null));
                });
    }


    private Mono<CustomResponse<MealSuggestionResponse>> saveMealSuggestions(List<MenuSuggestionResponse> suggestedMenuList,
                                                                             String orderDate, int mealId,
                                                                             List<Integer> companyIdList,
                                                                             int mealCount) {

        // Fetch and increment the counter from the sequence table
        Mono<Integer> counterMono = sequenceMasterRepository.findByPrefixEquals("PLANID")
                .flatMap(sequence -> {
                    sequence.setCounter(sequence.getCounter() + 1);
                    return sequenceMasterRepository.save(sequence)
                            .map(SequenceMaster::getCounter);
                });

        // List to keep track of saved mealSuggestionIds
        List<Integer> mealSuggestionIds = new ArrayList<>();

        // Find items without recipes and map the results to item names
        Flux<String> itemsWithoutRecipeNames = recipeMasterRepository.findItemNamesWithoutRecipe();

        // Convert Flux to a List
        Mono<List<String>> itemsWithoutRecipeList = itemsWithoutRecipeNames.collectList();

        // Lists to store items with and without recipes
        List<String> itemsWithoutRecipee = new ArrayList<>();
        List<MenuSuggestionResponse> menuSuggestion = new ArrayList<>();

        return counterMono.flatMap(planId -> {
            List<Integer> uniqueItemIds = suggestedMenuList.stream()
                    .flatMap(menuSuggestionResponse -> menuSuggestionResponse.getMealSuggestionDetails().stream())
                    .map(MenuSuggestionResponse.SuggestedItem::getItemId)
                    .distinct()
                    .toList();

            log.info("uniqueItemIds :: {}", uniqueItemIds);

            Mono<Map<Integer, BigDecimal>> itemQuantityMapper = calculateGrammage(orderDate, mealId, companyIdList,
                    uniqueItemIds)
                    .doOnError(e -> log.error("Error calculating grammage: {}", e.getMessage(), e));

            List<BigDecimal> resultArray = new ArrayList<>(); // To store results for each company

            Flux.fromIterable(companyIdList)
                    .flatMap(companyId ->
                            orderMasterRepository.findByCompanyIdAndMealIdAndOrderDate(companyId, mealId, orderDate)
                                    .flatMap(orderMaster -> {
                                        Integer dayOfWeek = orderMaster.getDayOfWeek();
                                        BigDecimal mealCounts = BigDecimal.valueOf(orderMaster.getMealCount());
                                        System.out.println(">> " + dayOfWeek + "___" + mealCounts);
                                        return getCostForCompany(companyId, mealId, dayOfWeek)
                                                .map(cost -> {
                                                    BigDecimal mealCost = mealCounts.multiply(cost);
                                                    resultArray.add(mealCost);
                                                    return mealCost;
                                                });
                                    })
                    )
                    .doOnComplete(() -> {
                        // Here, resultArray will contain the calculated meal costs for each company
                        for (int i = 0; i < companyIdList.size(); i++) {
                            log.info("Company {} - Meal cost: {}", companyIdList.get(i), resultArray.get(i));
                        }
                    })
                    .subscribe();

            return itemQuantityMapper.flatMap(itemMap -> chooseRecipeForItems(itemMap)
                    .doOnNext(itemRecipeMapper -> log.info("itemRecipeMapper: {}", itemRecipeMapper))
                    .onErrorResume(e -> {
                        log.error("Error choosing recipe for items: {}", e.getMessage(), e);
                        return Mono.empty();
                    })
                    .flatMap(itemRecipeMapper -> Flux.fromIterable(suggestedMenuList)
                            .flatMap(menuSuggestionResponse -> {
                                // Check if at least one item in the menu suggestion has a valid recipe ID
                                boolean atLeastOneItemHasRecipe = menuSuggestionResponse.getMealSuggestionDetails().stream()
                                        .anyMatch(suggestedItem -> {
                                            int itemId = suggestedItem.getItemId();
                                            return itemRecipeMapper.containsKey(itemId) && itemRecipeMapper.get(itemId) > 0;
                                        });

                                if (!atLeastOneItemHasRecipe) {
                                    log.warn("Skipping meal suggestion due to missing recipes for all items.");

                                    // Include items without a recipe in the "itemsWithoutRecipe" list
                                    menuSuggestionResponse.getMealSuggestionDetails().forEach(suggestedItem ->
                                            itemsWithoutRecipee.add(suggestedItem.getItemName()));

                                    return Mono.empty(); // Skip saving the meal suggestion
                                }

                                // Include items with a recipe in the "menuSuggestion" list
                                List<MenuSuggestionResponse.SuggestedItem> suggestedItemsWithRecipe = menuSuggestionResponse.getMealSuggestionDetails().stream()
                                        .filter(suggestedItem -> {
                                            int itemId = suggestedItem.getItemId();
                                            return itemRecipeMapper.containsKey(itemId) && itemRecipeMapper.get(itemId) > 0;
                                        })
                                        .collect(Collectors.toList());

                                menuSuggestionResponse.setMealSuggestionDetails(suggestedItemsWithRecipe);
                                menuSuggestion.add(menuSuggestionResponse);

                                MealSuggestion mealSuggestion = new MealSuggestion();
                                mealSuggestion.setCreatedBy(1);
                                mealSuggestion.setCreatedTime(Instant.now());
                                mealSuggestion.setActive(true);
                                mealSuggestion.setPlanId(planId);

                                // Save the mealSuggestion first to get the generated mealSuggestionId
                                return mealSuggestionRepository.save(mealSuggestion)
                                        .flatMap(savedMealSuggestion -> {
                                            int mealSuggestionId = savedMealSuggestion.getMealSuggestionId();
                                            log.info("MealSuggestion after saving: mealSuggestionId = {}", mealSuggestionId);

                                            // Save the mealSuggestionId in a list
                                            mealSuggestionIds.add(mealSuggestionId);

                                            List<MealSuggestionDetails> mealSuggestionDetailsList = new ArrayList<>();
                                            for (SuggestedItem suggestedItem : menuSuggestionResponse.getMealSuggestionDetails()) {
                                                int itemId = suggestedItem.getItemId();
                                                int recipeId = itemRecipeMapper.get(itemId);
                                                MealSuggestionDetails mealSuggestionDetails = new MealSuggestionDetails();
                                                mealSuggestionDetails.setItemId(itemId);
                                                mealSuggestionDetails.setRecipeId(recipeId);
                                                mealSuggestionDetails.setQuantity(itemMap.get(itemId));
                                                Integer uomId = (Integer) memcachedClient.get("itemUomMapper-" + itemId);
                                                if (uomId != null) {
                                                    mealSuggestionDetails.setUomId(uomId.intValue());
                                                } else {
                                                    log.error("UomId is null for item with itemId: {}", itemId);

                                                }
                                                mealSuggestionDetails.setGrammage(BigDecimal.valueOf(1.000));
                                                mealSuggestionDetails.setCreatedBy(1);
                                                mealSuggestionDetails.setCreatedTime(Instant.now());
                                                mealSuggestionDetails.setActive(true);
                                                mealSuggestionDetails.setMealSuggestionId(mealSuggestionId);
                                                mealSuggestionDetailsList.add(mealSuggestionDetails);
                                            }

                                            // Save all mealSuggestionDetails for the same mealSuggestionId
                                            return mealSuggestionDetailsRepository.saveAll(mealSuggestionDetailsList)
                                                    .then(Mono.just(mealSuggestionId)) // Return the mealSuggestionId
                                                    .flatMap(savedId -> totalCostForMealSuggestion(savedId)
                                                            .doOnNext(mealSuggestionTotalCost -> {
                                                                BigDecimal sum = resultArray.stream()
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                                                log.info("Sum: {}", sum);
                                                                totalCostForMealSuggestion(savedId); // Log total cost

                                                                BigDecimal perMealCost = mealSuggestionTotalCost.divide(
                                                                        BigDecimal.valueOf(mealCount), 2, RoundingMode.HALF_UP);
                                                                log.info("perMealCost: {} mealSuggestionTotalCost: {}", perMealCost, mealSuggestionTotalCost);

                                                                BigDecimal salePerMealCost = sum.divide(BigDecimal.valueOf(mealCount), 2, RoundingMode.HALF_UP);
                                                                BigDecimal salePercentage = perMealCost.divide(salePerMealCost, 4, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100));

                                                                log.info("Total Sale Meal cost: {} perMealCost: {} Sale PerMealCost: {} salePercentage: {}",
                                                                        sum, perMealCost, salePerMealCost, salePercentage.setScale(2, RoundingMode.DOWN) + "%");


                                                                menuSuggestionResponse.setTotalMakingCost(mealSuggestionTotalCost);
                                                                menuSuggestionResponse.setCostPerMeal(perMealCost);
                                                                menuSuggestionResponse.setPercentOfSaleCost(salePercentage.setScale(2, RoundingMode.DOWN) + "%");
                                                            })
                                                            .thenReturn(savedId));
                                        })
                                        .onErrorResume(e -> {
                                            log.error("Error saving meal suggestion: {}", e.getMessage(), e);
                                            return Mono.empty();
                                        });
                            })
                            .collectList()
                            .onErrorResume(e -> {
                                log.error("Error collecting meal suggestion ids: {}", e.getMessage(), e);
                                return Mono.empty();
                            })
                            .flatMap(savedIds -> {
                                // Create the MealSuggestionResponse
                                mealSuggestionIds.sort(Comparator.naturalOrder());//sort the order
                                List<MenuSuggestionResponse> menuSuggestionResponses = new ArrayList<>();
                                for (int i = 0; i < mealSuggestionIds.size(); i++) {
                                    MenuSuggestionResponse menuSuggestionResponse = suggestedMenuList.get(i);
                                    menuSuggestionResponse.setMealSuggestionId(mealSuggestionIds.get(i));

                                    // Set the quantity for each SuggestedItem
                                    List<MenuSuggestionResponse.SuggestedItem> suggestedItems = menuSuggestionResponse.getMealSuggestionDetails();
                                    for (MenuSuggestionResponse.SuggestedItem suggestedItem : suggestedItems) {
                                        BigDecimal quantity = itemMap.getOrDefault(suggestedItem.getItemId(), BigDecimal.ZERO);
                                        suggestedItem.setQuantity(quantity);
                                    }
                                    menuSuggestionResponses.add(menuSuggestionResponse);
                                }

                                Collections.sort(menuSuggestionResponses, Comparator.comparingDouble(menuSuggestionResponse -> {

                                    // Parse the percentOfSaleCost string and remove the '%' sign
                                    String percentOfSaleCost = menuSuggestionResponse.getPercentOfSaleCost();
                                    percentOfSaleCost = percentOfSaleCost.replaceAll("%", "");

                                    // Convert the cleaned percentOfSaleCost to a double for comparison
                                    return Double.parseDouble(percentOfSaleCost);
                                }));

                                return itemsWithoutRecipeList.flatMap(itemsWithoutRecipe -> {
                                    MealSuggestionResponse response = new MealSuggestionResponse(mealId, itemsWithoutRecipe, menuSuggestionResponses);
                                    return Mono.just(new CustomResponse<>(new Status(200, "Success"), response));
                                });
                            })));
        });
    }

    private Mono<BigDecimal> getCostForCompany(Integer companyId, Integer mealId, Integer dayOfWeek) {
        return companyCostMappingRepository.findByCompanyIdAndMealIdAndDay(companyId, mealId, dayOfWeek)
                .map(CompanyMealCostMapping::getCost)
                .switchIfEmpty(Mono.just(BigDecimal.valueOf(0.0)));
    }


    private Mono<BigDecimal> totalCostForMealSuggestion(int mealSuggestionId) {
        return mealSuggestionDetailsRepository.findByMealSuggestionId(mealSuggestionId)
                .flatMapSequential(detail -> {
                    int itemId = detail.getItemId();

                    return recipeDetailsRepository.findByRecipeId(detail.getRecipeId())
                            .flatMap(recipeDetail -> {
                                BigDecimal ingredientCost = recipeDetail.getIngredientCost();
                                BigDecimal itemTotalCost = BigDecimal.ZERO;

                                if (ingredientCost != null) {
                                    itemTotalCost = ingredientCost;
                                }
                                return Mono.just(itemTotalCost);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .map(totalCost -> {
                                log.info("Total cost for meal suggestion {} itemid:{} recipe id:{} is: {}",
                                        mealSuggestionId, itemId, detail.getRecipeId(), totalCost);
                                return totalCost;
                            });
                })
                .collectList()
                .map(totalCosts -> totalCosts.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .doOnSuccess(combinedTotalCost -> log.info("Combination Total cost for meal suggestion {} is: {}",
                        mealSuggestionId, combinedTotalCost));
    }


    public Flux<MealSuggestionDetails> getMealSuggestionDetailsBySuggestionId(int mealSuggestionId) {
        return mealSuggestionDetailsRepository.findByMealSuggestionId(mealSuggestionId)
                .flatMap(detail -> calculateTotalCostForMealSuggestion(detail)
                        .flatMap(totalCost -> {
                            detail.setTotalCost(totalCost);

                            return totalCostForMealSuggestion(mealSuggestionId)
                                    .map(combinedTotalCost -> {
                                        BigDecimal contribution = BigDecimal.ZERO;
                                        if (combinedTotalCost.compareTo(BigDecimal.ZERO) > 0) {
                                            contribution = totalCost.divide(combinedTotalCost, 2, RoundingMode.HALF_UP)
                                                    .multiply(BigDecimal.valueOf(100));
                                        }
                                        detail.setContribution(contribution.toPlainString() + "%");

                                        BigDecimal rateUnit = totalCost.divide(detail.getQuantity(), 2, RoundingMode.HALF_UP);
                                        detail.setUnitRate(rateUnit);

                                        return detail;
                                    });
                        }))
                .flatMap(detail -> itemRepository.findById(detail.getItemId())
                        .map(item -> {
                            detail.setItemName(item.getItemName());
                            return detail;
                        })
                        .defaultIfEmpty(detail))
                .collectList() // Collect the elements into a list
                .flatMapMany(list -> Flux.fromIterable(list) // Convert the list back to a Flux
                        .sort(Comparator.comparingInt(MealSuggestionDetails::getMealSuggestionDetailsId))); // Sort by mealSuggestionDetailsId
    }


    private Mono<BigDecimal> calculateTotalCostForMealSuggestion(MealSuggestionDetails detail) {
        int itemId = detail.getItemId();

        return recipeDetailsRepository.findByRecipeId(detail.getRecipeId())
                .flatMap(recipeDetail -> {
                    int ingredientId = recipeDetail.getIngredientId();

                    // Fetch the corresponding ingredient from the Ingredients table
                    return ingredientsRepository.findById(ingredientId)
                            .map(ingredient -> {
                                BigDecimal ingredientCost = ingredient.getCost();
                                BigDecimal itemTotalCost = BigDecimal.ZERO;

                                if (ingredientCost != null) {
                                    itemTotalCost = ingredientCost;
                                }

                                log.info("Total cost for meal suggestion {} itemid:{} recipe id:{} is: {}",
                                        detail.getMealSuggestionId(), itemId, detail.getRecipeId(), itemTotalCost);

                                return itemTotalCost;
                            });
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private Mono<Map<Integer, BigDecimal>> calculateGrammage(String orderDate, int mealId, List<
            Integer> companyIdList, List<Integer> itemIdList) {
        LocalDate dataParam = LocalDate.parse(orderDate, DateTimeFormatter.ISO_DATE);
        Flux<OrderMaster> ordersFlux = orderMasterRepository.findByMealIdAndOrderDateAndCompanyIdIn(mealId, dataParam, companyIdList);

        return ordersFlux.collectList().map(ordersList -> {
            Map<Integer, BigDecimal> itemQuantityMapper = new HashMap<>();
            for (int itemId : itemIdList) {
                BigDecimal itemQuantity = BigDecimal.ZERO;
                for (OrderMaster orderMaster : ordersList) {
                    String mapKey = orderMaster.getCompanyId() + "-" + itemId + "-" + dataParam.getDayOfWeek().getValue();
                    BigDecimal grammage = (BigDecimal) memcachedClient.get("grammageStore-" + mapKey);
                    //TODO: How to handle if no Grammage info available for input data, Send report of such data
                    if (grammage == null) {
                        grammage = BigDecimal.ONE;
                    }

                    // Log the values for debugging
                    System.out.println("grammage: " + grammage);
                    System.out.println("order.getMealCount(): " + orderMaster.getMealCount());

                    BigDecimal mealCount = BigDecimal.valueOf(orderMaster.getMealCount());
                    itemQuantity = itemQuantity.add(mealCount.multiply(grammage));

                }
                itemQuantityMapper.put(itemId, itemQuantity);
            }
            return itemQuantityMapper;
        });
    }

    private List<MenuSuggestionResponse> getAllPossibleMealCombos(Map<Integer, List<MealItemMapping>> categoryItemsMapper, Map<Integer, Integer> categoryCounts) {
        List<MenuSuggestionResponse> suggestions = new ArrayList<>();
        List<ItemMenuResponse> currentCombination = new ArrayList<>();
        List<Integer> categoryIds = new ArrayList<>(categoryItemsMapper.keySet());
        Set<Integer> usedItemIds = new HashSet<>(); // Initialize the set
        Map<Integer, Integer> itemCounts = new HashMap<>(); // Initialize itemCounts map

        for (int categoryId : categoryCounts.keySet()) {
            itemCounts.put(categoryId, 0); // Initialize counts for each category to 0
        }

        generateCombinations(categoryItemsMapper, categoryCounts, categoryIds, 0, currentCombination, suggestions, usedItemIds, itemCounts); // Pass the itemCounts map as an argument
        return suggestions;
    }


    private void generateCombinations(Map<Integer, List<MealItemMapping>> categoryItemsMapper,
                                      Map<Integer, Integer> categoryCounts, List<Integer> categoryIds, int index,
                                      List<ItemMenuResponse> currentCombination, List<MenuSuggestionResponse> suggestions,
                                      Set<Integer> usedItemIds, Map<Integer, Integer> itemCounts) {
        log.info("CategoryCount:{}", categoryCounts);
        if (index == categoryIds.size()) {
            List<MenuSuggestionResponse.SuggestedItem> suggestedItems = currentCombination.stream()
                    .map(item -> {
                        int categoryId = item.getCategoryId();
                        String categoryName = (String) memcachedClient.get("categoryNames-" + categoryId);
                        BigDecimal quantity = item.getPlannedQuantity();

                        return new MenuSuggestionResponse.SuggestedItem(item.getItemId(), item.getItemName(), categoryId, categoryName, quantity);
                    })
                    .toList();

            suggestions.add(new MenuSuggestionResponse(suggestedItems));
            return;
        }

        int categoryId = categoryIds.get(index);
        List<MealItemMapping> itemsInCategory = categoryItemsMapper.get(categoryId);

        if (itemsInCategory != null) {
            int count = categoryCounts.get(categoryId);
            generateItemsForCategory(categoryItemsMapper, categoryCounts, categoryIds, index, currentCombination, suggestions, count, categoryId, usedItemIds, itemCounts);
        } else {
            generateCombinations(categoryItemsMapper, categoryCounts, categoryIds, index + 1, currentCombination, suggestions, usedItemIds, itemCounts);
        }
    }

    private void generateItemsForCategory(Map<Integer, List<MealItemMapping>> categoryItemsMapper,
                                          Map<Integer, Integer> categoryCounts, List<Integer> categoryIds, int index,
                                          List<ItemMenuResponse> currentCombination, List<MenuSuggestionResponse> suggestions,
                                          int remainingCount, int categoryId, Set<Integer> usedItemIds, Map<Integer, Integer> itemCounts) {
        if (remainingCount == 0) {
            generateCombinations(categoryItemsMapper, categoryCounts, categoryIds, index + 1, currentCombination, suggestions, usedItemIds, itemCounts);
            return;
        }

        List<MealItemMapping> itemsInCategory = categoryItemsMapper.get(categoryId);

        for (MealItemMapping itemMapping : itemsInCategory) {
            if (!usedItemIds.contains(itemMapping.getItemId()) && itemCounts.get(categoryId) < categoryCounts.get(categoryId)) {
                String itemName = (String) memcachedClient.get("itemNames-" + itemMapping.getItemId());
                currentCombination.add(new ItemMenuResponse(itemMapping.getItemId(), itemName, itemMapping.getPlannedQuantity(), categoryId));
                usedItemIds.add(itemMapping.getItemId());
                itemCounts.put(categoryId, itemCounts.get(categoryId) + 1);

                // Recurse with remainingCount - 1 for the next item
                generateItemsForCategory(categoryItemsMapper, categoryCounts, categoryIds, index, currentCombination, suggestions, remainingCount - 1, categoryId, usedItemIds, itemCounts);

                // Remove the last item to backtrack
                currentCombination.remove(currentCombination.size() - 1);
                usedItemIds.remove(itemMapping.getItemId());
                itemCounts.put(categoryId, itemCounts.get(categoryId) - 1);
            }
        }
    }


    public Mono<IngredientSummaryMenuResponse> getMealSuggestionIngredientSummary(int mealSuggestionId, int mealCount) {
        return mealSuggestionDetailsRepository.findByMealSuggestionId(mealSuggestionId)
                .collectList()
                .flatMap(detailsList -> {
                    // Sort detailsList by mealSuggestionDetailsId
                    detailsList.sort(Comparator.comparingInt(MealSuggestionDetails::getMealSuggestionDetailsId));

                    Map<Integer, BigDecimal> ingredientIdToTotalQuantityMap = new HashMap<>();
                    Map<Integer, String> itemIdToNameMap = new HashMap<>();
                    Map<Integer, BigDecimal> itemIdToQuantityMap = new HashMap<>();
                    Map<Integer, BigDecimal> itemIdToWeightMap = new HashMap<>();
                    Map<Integer, String> ingredientIdToUomNameMap = new HashMap<>();

                    List<BigDecimal> resultBigDecimalList = new ArrayList<>();

                    return Flux.fromIterable(detailsList)
                            .flatMapSequential(details -> {
                                int itemId = details.getItemId();
                                int recipeId = details.getRecipeId();
                                String itemName = (String) memcachedClient.get("itemNames-" + itemId);
                                BigDecimal quantity = details.getQuantity();
                                Mono<BigDecimal> costMono = calculateTotalCostMealSuggestion(recipeId)
                                        .map(costString -> costString);
                                costMono.subscribe(cost -> {
                                    System.out.println("item Cost: " + cost + "" + mealCount);
                                    resultBigDecimalList.add(cost);
                                });

                                return itemRepository.findById(itemId)
                                        .flatMap(item -> {
                                            BigDecimal itemWeight;

                                            itemWeight = item.getGram().multiply(item.getWeight());

                                            itemIdToNameMap.put(itemId, itemName);
                                            itemIdToQuantityMap.put(itemId, quantity);
                                            itemIdToWeightMap.put(itemId, itemWeight);

                                            // Use Mono.from to convert the Flux to Mono
                                            return Mono.from(recipeDetailsRepository.findByRecipeId(recipeId))
                                                    .flatMap(recipeDetails -> {
                                                        int ingredientId = recipeDetails.getIngredientId();
                                                        BigDecimal ingredientQuantity = recipeDetails.getQuantity();

                                                        ingredientIdToTotalQuantityMap.merge(ingredientId, ingredientQuantity, BigDecimal::add);

                                                        return Mono.just(ingredientId);
                                                    });
                                        });
                            })
                            .collectList()
                            .flatMap(ingredientIds -> {
                                // Fetch UOM names using the fetched ingredient IDs
                                List<Mono<Void>> uomFetchMonos = ingredientIds.stream()
                                        .map(ingredientId -> ingredientsRepository.findById(ingredientId)
                                                .flatMap(ingredient -> uomMasterRepository.findById(ingredient.getUomId()))
                                                .doOnNext(uom -> ingredientIdToUomNameMap.put(ingredientId, uom.getName()))
                                                .then())
                                        .collect(Collectors.toList());

                                return Mono.when(uomFetchMonos)
                                        .thenReturn(ingredientIdToTotalQuantityMap);
                            })
                            .map(ignored -> {
                                List<Map<String, Object>> items = new ArrayList<>();
                                List<Map<String, Object>> ingredients = new ArrayList<>();


                                BigDecimal sum = BigDecimal.ZERO;
                                for (BigDecimal value : resultBigDecimalList) {
                                    sum = sum.add(value);
                                }
                                // Print the sum
                                System.out.println("Sum of Total cost: " + sum + "//" + mealCount);
                                BigDecimal perMealCost = sum.divide(BigDecimal.valueOf(mealCount), 2, RoundingMode.HALF_UP);
                                System.out.println("totalCostPerMeal: " + sum.divide(BigDecimal.valueOf(mealCount), 2, RoundingMode.HALF_UP));


                                for (Map.Entry<Integer, String> entry : itemIdToNameMap.entrySet()) {
                                    int itemId = entry.getKey();
                                    String itemName = entry.getValue();
                                    BigDecimal itemQuantity = itemIdToQuantityMap.get(itemId);
                                    BigDecimal itemWeight = itemIdToWeightMap.get(itemId);

                                    Map<String, Object> itemMap = new HashMap<>();
                                    itemMap.put("itemid", itemId);
                                    itemMap.put("itemName", itemName);
                                    itemMap.put("quantity", itemQuantity);
                                    itemMap.put("itemWeight", itemWeight);
                                    items.add(itemMap);
                                }


                                // Sort items within detailsList by mealSuggestionDetailsId
                                items.sort(Comparator.comparingInt(item -> detailsList.stream()
                                        .filter(detail -> detail.getItemId() == (Integer) item.get("itemid"))
                                        .findFirst()
                                        .orElse(new MealSuggestionDetails())
                                        .getMealSuggestionDetailsId()));

                                for (Map.Entry<Integer, BigDecimal> entry : ingredientIdToTotalQuantityMap.entrySet()) {
                                    int ingredientId = entry.getKey();
                                    BigDecimal totalQuantity = entry.getValue();
                                    String ingredientName = (String) memcachedClient.get("ingredientNames-" + ingredientId);
                                    String uomName = ingredientIdToUomNameMap.get(ingredientId);

                                    Map<String, Object> ingredientMap = new HashMap<>();
                                    ingredientMap.put("ingredientName", ingredientName);
                                    ingredientMap.put("name", uomName);
                                    ingredientMap.put("Quantity", totalQuantity);
                                    ingredients.add(ingredientMap);
                                }

                                BigDecimal totalItemWeight = items.stream()
                                        .map(item -> (BigDecimal) item.get("itemWeight"))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);


                                List<IngredientSummaryMenuResponse.ItemIngredient> itemIngredients = new ArrayList<>();
//                                itemIngredients.add(new IngredientSummaryMenuResponse.ItemIngredient(totalItemWeight, items, ingredients));

                                itemIngredients.add(new IngredientSummaryMenuResponse.ItemIngredient(perMealCost, totalItemWeight, items, ingredients));
                                return new IngredientSummaryMenuResponse(itemIngredients);
                            });
                });
    }


    private Mono<BigDecimal> calculateTotalCostMealSuggestion(int recipeId) {
        // Retrieve recipe details for the given recipeId
        return recipeDetailsRepository.findByRecipeId(recipeId)
                .flatMap(recipeDetail -> {
                    int ingredientId = recipeDetail.getIngredientId();

                    // Fetch the corresponding ingredient from the Ingredients table
                    return ingredientsRepository.findById(ingredientId)
                            .map(ingredient -> {
                                BigDecimal cost = ingredient.getCost();
                                BigDecimal quantity = recipeDetail.getQuantity();

                                // Calculate the cost by multiplying ingredient cost with quantity
                                if (cost != null && quantity != null) {
                                    return cost.multiply(quantity);
                                } else {
                                    return BigDecimal.ZERO;
                                }
                            });
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


}



