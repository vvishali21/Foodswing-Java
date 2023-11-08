package com.meteoriqs.foodswing.common.mappings;

import com.meteoriqs.foodswing.data.entity.MediumMaster;
import com.meteoriqs.foodswing.data.entity.ProcessMaster;
import com.meteoriqs.foodswing.data.entity.RecipeMaster;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.data.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.spy.memcached.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;

@Component
@Scope(value = "singleton")
@Getter
@Log4j2
public class ConfigurationHolder {
//    TODO: Change to Memcached - Implement changes in all places
//    TODO: Creating new company - insert values in Grammage (new requirement ) - Completed
//    TODO: Duplicate check in Order creation - based on date, meal type and company - Completed
//    TODO: Changes in MealType Master - Option to choose categories, save in a new table - meal_category_mapping,
//     columns - meal_type_id, category_id - Completed
//    TODO : Item master screen - new field 'Weight / Item' for UOM value 'Count' - default NULL,
//     show in meal suggestion summary, calculate weight of total meal suggestion
//    TODO: Show plans, use data from order table (data with meal_suggestion_id > 0), meal suggestion table,
//     delete plan (update orders set meal_suggestion_id = 0)
//     (view pop-up - same view as meal suggestion details screen)
//    TODO: Planning - Show orders - Show only entries with meal_suggestion_id = 0
//    TODO: Planning - Choose category Id List based on mealTypeId - currently hardcoded (1,2,3,4)
//    TODO: Meal summary - show cost / meal, show weight, cost as % of sale cost
//    TODO: Indent changes - Show company wise item count ????
//    Order (count), Meal suggestion details (itemId), Grammage (day, companyId -> gram)
//    Company A - Idly 100, Tea 150
//    Company B - Idly 400, Tea 600
//    Company C - Idly 300, Tea 450
//    TODO: Suggestion Edit flow
//    TODO: Plan Edit - Remove a few companies
//    TODO: Vessel ??? Suresh, Justin
//    TODO: Confirm UOM List - need for weight calculation
//    TODO: Company screen - get cost config for each meal type (default price)
//    New screen - Company cost config ( like grammage ... company Id, meal ID, dayOfWeek, cost ) only cost is editable here

    private final ItemRepository itemRepository;
    private final IngredientsRepository ingredientsRepository;
    private final CategoryRepository categoryRepository;
    private final GrammageRepository grammageRepository;
    private final MealRepository mealRepository;
    private final CompanyRepository companyRepository;
    private final RecipeMasterRepository recipeMasterRepository;
    private final UomMasterRepository uomMasterRepository;
    private final ProcessMasterRepository processMasterRepository;
    private final MediumMasterRepository mediumMasterRepository;
    private final VesselRepository vesselRepository;

    private final MemcachedClient memcachedClient;

    @Autowired
    public ConfigurationHolder(ItemRepository itemRepository,
                               IngredientsRepository ingredientsRepository,
                               CategoryRepository categoryRepository,
                               GrammageRepository grammageRepository,
                               RecipeMasterRepository recipeMasterRepository,
                               UomMasterRepository uomMasterRepository,
                               ProcessMasterRepository processMasterRepository,
                               MediumMasterRepository mediumMasterRepository,
                               MealRepository mealRepository, CompanyRepository companyRepository,
                               VesselRepository vesselRepository,
                               MemcachedClient memcachedClient) {
        this.itemRepository = itemRepository;
        this.ingredientsRepository = ingredientsRepository;
        this.categoryRepository = categoryRepository;
        this.grammageRepository = grammageRepository;
        this.recipeMasterRepository = recipeMasterRepository;
        this.uomMasterRepository = uomMasterRepository;
        this.processMasterRepository = processMasterRepository;
        this.mediumMasterRepository = mediumMasterRepository;
        this.mealRepository = mealRepository;
        this.companyRepository = companyRepository;
        this.vesselRepository = vesselRepository;
        this.memcachedClient = memcachedClient;
    }

    @PostConstruct
    public void loadValues() {
        Flux<Item> itemsFlux = itemRepository.findAll();
        itemsFlux
                .collectMap(Item::getItemId, Item::getItemName)
                .doOnNext(itemMap -> itemMap.forEach((key, value) ->
                        memcachedClient.set("itemNames-"+key, 0, value)))
                .subscribe();

        itemsFlux
                .map(this::convertToItemSearch)
                .collectList()
                .doOnNext(recipeSearchResponses ->
                        memcachedClient.set("itemEntityList", 0, recipeSearchResponses))
                .subscribe();

        itemsFlux.collectMap(Item::getItemId, Item::getUomId)
                .doOnNext(itemMap-> itemMap.forEach((key,value)->
                        memcachedClient.set("itemUomMapper-"+key,0, value)))
                .subscribe();

        itemsFlux.collectMap(Item::getItemId, Item::getCategoryId)
                .doOnNext(itemCategoryMap-> itemCategoryMap.forEach((key,value)->
                        memcachedClient.set("itemCategoryMapper-"+key,0, value)))
                .subscribe();

        itemsFlux.collectMap(Item::getItemId, Item::getGram)
                .doOnNext(itemGramMap-> itemGramMap.forEach((key,value)->
                        memcachedClient.set("itemGramMapper-"+key,0, value)))
                .subscribe();

        Flux<Meal> mealsFlux = mealRepository.findAll();
        mealsFlux
                .collectMap(Meal::getMealId, Meal::getName)
                .doOnNext(mealMap-> mealMap.forEach((key, value)->
                        memcachedClient.set("mealNames-"+key,0, value)))
                .subscribe();

        mealsFlux
                .map(this::convertToMealSearch)
                .collectList()
                .doOnNext(recipeSearchResponses ->
                        memcachedClient.set("mealEntityList", 0, recipeSearchResponses))
                .subscribe();

        Flux<Company> companyFlux = companyRepository.findAll();
        companyFlux
                .collectMap(Company::getCompanyId, Company::getCompanyName)
                .doOnNext(companyMap-> companyMap.forEach((key, value)->
                        memcachedClient.set("companyNames-"+key,0, value)))
                .subscribe();

        companyFlux
                .map(this::convertToCompanySearch)
                .collectList()
                .doOnNext(recipeSearchResponses ->
                        memcachedClient.set("companyEntityList", 0, recipeSearchResponses))
                .subscribe();

        Flux<Category> categoryFlux = categoryRepository.findAll();
        categoryFlux
                .collectMap(Category::getCategoryId, Category::getCategoryName)
                .doOnNext(categoryMap-> categoryMap.forEach((key, value)->
                        memcachedClient.set("categoryNames-"+key,0, value)))
                .subscribe();

        categoryFlux
                .map(this::convertToCategorySearch)
                .collectList()
                .doOnNext(recipeSearchResponses ->
                        memcachedClient.set("categoryEntityList", 0, recipeSearchResponses))
                .subscribe();

        Flux<Vessel> vesselFlux = vesselRepository.findAll();
        vesselFlux
                .collectMap(Vessel::getVesselId, Vessel::getVesselName)
                .doOnNext(vesselMap-> vesselMap.forEach((key, value)->
                        memcachedClient.set("vesselNames-"+key,0, value)))
                .subscribe();

        Flux<RecipeMaster> recipeMasterFlux = recipeMasterRepository.findAll();
        recipeMasterFlux
                .collectMap(RecipeMaster::getRecipeId, RecipeMaster::getRecipeDescription)
                .doOnNext(recipeMap-> recipeMap.forEach((key, value)->{
                    if (value == null) {
                        value = "N/A";
                    }
                    memcachedClient.set("recipeDescription-"+key,0, value);
                }))
                .subscribe();

        Flux<UomMasterEntity> uomMasterFlux = uomMasterRepository.findAll();
        uomMasterFlux
                .collectMap(UomMasterEntity::getUomId, UomMasterEntity::getName)
                .doOnNext(uomMap -> uomMap.forEach((key, value) ->
                        memcachedClient.set("uomNames-"+key, 0, value)))
                .subscribe();

        uomMasterFlux
                .map(this::convertToUomSearch)
                .collectList()
                .doOnNext(recipeSearchResponses ->
                        memcachedClient.set("uomEntityList", 0, recipeSearchResponses))
                .subscribe();

        Flux<ProcessMaster> processFlux = processMasterRepository.findAll();
        processFlux
                .collectMap(ProcessMaster::getProcessId, ProcessMaster::getProcessName)
                .doOnNext(processMap -> processMap.forEach((key, value) -> memcachedClient.set("processNames-"+key, 0, value)))
                .subscribe();

        processFlux
                .map(this::convertToProcessSearch)
                .collectList()
                .doOnNext(recipeSearchResponses -> {
                    memcachedClient.set("processEntityList", 0, recipeSearchResponses);
                })
                .subscribe();

        processFlux
                .collectMap(ProcessMaster::getProcessId, ProcessMaster::getProcessName)
                .doOnNext(processMap -> processMap.forEach((key, value) -> memcachedClient.set("recipeProcessNames-"+key, 0, value)))
                .subscribe();


        Flux<MediumMaster> mediumFlux = mediumMasterRepository.findAll();
        mediumFlux
                .collectMap(MediumMaster::getMediumId, MediumMaster::getMediumName)
                .doOnNext(mediumMap -> mediumMap.forEach((key, value) ->
                        memcachedClient.set("mediumNames-"+key, 0, value)))
                .subscribe();

        mediumFlux
                .map(this::convertToMediumSearch)
                .collectList()
                .doOnNext(recipeSearchResponses ->
                        memcachedClient.set("mediumEntityList", 0, recipeSearchResponses))
                .subscribe();

        mediumFlux
                .collectMap(MediumMaster::getMediumId, MediumMaster::getMediumName)
                .doOnNext(mediumMap -> mediumMap.forEach((key, value) ->
                        memcachedClient.set("recipeMediumNames-"+key, 0, value)))
                .subscribe();


        Flux<Ingredients> ingredientsFlux = ingredientsRepository.findAll();
        ingredientsFlux
                .collectMap(Ingredients::getIngredientId, Ingredients::getCost)
                .doOnNext(ingredientCostMap -> ingredientCostMap.forEach((key, value) ->
                        memcachedClient.set("ingredientCost-"+key, 0, value)))
                .subscribe();

        ingredientsFlux
                .collectMap(Ingredients::getIngredientId, Ingredients::getUomId)
                .doOnNext(ingredientUomMap -> ingredientUomMap.forEach((key, value) ->
                        memcachedClient.set("ingredientUomMapper-"+key, 0, value)))
                .subscribe();

        ingredientsFlux
                .collectMap(Ingredients::getIngredientId, Ingredients::getIngredientName)
                .doOnNext(ingredientMap -> ingredientMap.forEach((key, value) ->
                        memcachedClient.set("ingredientNames-"+key, 0, value)))
                .subscribe();

        ingredientsFlux
                .map(this::convertToIngredientSearch)
                .collectList()
                .doOnNext(recipeSearchResponses ->
                        memcachedClient.set("ingredientEntityList", 0, recipeSearchResponses))
                .subscribe();

        Flux<Grammage> grammgeFlux = grammageRepository.findAll();
        grammgeFlux.collectMap(this::generateGrammageKey, Grammage::getGram)
                .doOnNext(grammageMap -> grammageMap.forEach((key, value) ->
                        memcachedClient.set("grammageStore-"+key, 0, value)))
                .subscribe();

    }


    private String generateGrammageKey(Grammage grammage) {
        return grammage.getCompanyId() + "-" + grammage.getItemId() + "-" + grammage.getDay();
    }

    private RecipeSearchResponse convertToProcessSearch(ProcessMaster processMaster) {
        RecipeSearchResponse response = new RecipeSearchResponse();
        response.setId(processMaster.getProcessId());
        response.setName(processMaster.getProcessName());
        response.setUomId(0);
        response.setUomName(null);
        response.setCost(null);
        return response;
    }

    private RecipeSearchResponse convertToMediumSearch(MediumMaster mediumMaster) {
        RecipeSearchResponse response = new RecipeSearchResponse();
        response.setId(mediumMaster.getMediumId());
        response.setName(mediumMaster.getMediumName());
        response.setUomId(0);
        response.setUomName(null);
        response.setCost(null);
        return response;
    }

    private RecipeSearchResponse convertToUomSearch(UomMasterEntity uomMasterEntity) {
        RecipeSearchResponse response = new RecipeSearchResponse();
        response.setId(uomMasterEntity.getUomId());
        response.setName(uomMasterEntity.getName());
        response.setUomId(0);
        response.setUomName(null);
        response.setCost(null);
        return response;
    }

    private RecipeSearchResponse convertToMealSearch(Meal meal) {
        RecipeSearchResponse response = new RecipeSearchResponse();
        response.setId(meal.getMealId());
        response.setName(meal.getName());
        response.setUomId(0);
        response.setUomName(null);
        response.setCost(null);
        return response;
    }

    private RecipeSearchResponse convertToCategorySearch(Category category) {
        RecipeSearchResponse response = new RecipeSearchResponse();
        response.setId(category.getCategoryId());
        response.setName(category.getCategoryName());
        response.setUomId(0);
        response.setUomName(null);
        response.setCost(null);
        return response;
    }

    private RecipeSearchResponse convertToCompanySearch(Company company) {
        RecipeSearchResponse response = new RecipeSearchResponse();
        response.setId(company.getCompanyId());
        response.setName(company.getCompanyName());
        response.setUomId(0);
        response.setUomName(null);
        response.setCost(null);
        return response;
    }

    private RecipeSearchResponse convertToItemSearch(Item item) {
        RecipeSearchResponse response = new RecipeSearchResponse();
        response.setId(item.getItemId());
        response.setName(item.getItemName());
        response.setUomId(0);
        response.setUomName(null);
        response.setCost(null);
        return response;
    }

    private RecipeSearchResponse convertToIngredientSearch(Ingredients ingredients) {
        RecipeSearchResponse response = new RecipeSearchResponse();
        response.setId(ingredients.getIngredientId());
        response.setName(ingredients.getIngredientName());
        response.setUomId(ingredients.getUomId());
        response.setUomName((String) memcachedClient.get("uomNames-"+ingredients.getUomId()));
        response.setCost((BigDecimal) memcachedClient.get("ingredientCost-"+ingredients.getIngredientId()));
        return response;
    }

}
