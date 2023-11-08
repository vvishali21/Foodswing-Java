package com.meteoriqs.foodswing.production.config;

import com.meteoriqs.foodswing.production.controller.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration(proxyBeanMethods = false)
public class RoutingConfig {

    private static final RequestPredicate ACCEPT_JSON = accept(MediaType.APPLICATION_JSON);

    @Bean
    public RouterFunction<ServerResponse> allRoutes(
            UserController userController,
            CompanyController companyController,
            ItemController itemController,
            IngredientController ingredientController,
            MealController mealController,
            RecipeController recipeController,
            ErrorLogController errorLogController,
            CategoryController categoryController,
            ProcessController processController,
            MediumController mediumController,
            UomController uomController,
            OrderController orderController,
            VesselController vesselController,
            MealItemMappingController mealItemMappingController,
            GrammageController grammageController,
            VesselItemMappingController vesselItemMappingController
    )
    {
        return RouterFunctions.route()
                //Error Log Post API
                .POST("/api/errorLog",ACCEPT_JSON,errorLogController::createErrorLog)
                //Sample API for Create-User
                .POST("/api/production/createUser", ACCEPT_JSON, userController::createUser)


                //Company
                .GET("/api/company", ACCEPT_JSON, companyController::getAllCompany)
                .GET("/api/company/{companyId}", companyController::getCompanyById)
                .POST("/api/createCompany", ACCEPT_JSON, companyController::createCompany)
                .PUT("/api/updateCompany/{companyId}", ACCEPT_JSON, companyController::updateCompany)
                .DELETE("/api/deleteCompany/{companyId}", companyController::deleteCompany)
                .GET("/api/companyAllGet/list", recipeController::findAllCompany)


                //Item
                .GET("/api/item", ACCEPT_JSON, itemController::getAllItems)
                .GET("/api/item/{id}", itemController::getItemById)
                .POST("/api/item/createItem", ACCEPT_JSON, itemController::createItem)
                .PUT("/api/updateItem/{id}", ACCEPT_JSON, itemController::updateItem)
                .DELETE("/api/deleteItem/{id}", itemController::deleteItem)
                .GET("/api/itemName", recipeController::findItemByPartialName)
                .GET("/api/itemAllGet/list", recipeController::findAllItem)


                //Meal
                .GET("/api/meal", ACCEPT_JSON, mealController::getAllMeal)
                .GET("/api/meal/{id}", mealController::getMealById)
                .POST("/api/meal/createMeal", ACCEPT_JSON, mealController::createMeal)
                .PUT("/api/meal/{id}", ACCEPT_JSON, mealController::updateMeal)
                .DELETE("/api/deleteMeal/{id}", mealController::deleteMeal)
                .GET("/api/mealAllGet/list", recipeController::findAllMeal)


                //Ingredient
                .GET("/api/sampleDemoIngredient", ingredientController::getAllIngredientsSample)
                .GET("/api/ingredient", ingredientController::getAllIngredients)
                .GET("/api/ingredient/{ingredientId}", ingredientController::getIngredientById)
                .DELETE("/api/deleteIngredient/{ingredientId}", ingredientController::deleteIngredient)
                .POST("/api/createIngredient", ACCEPT_JSON, ingredientController::createIngredient)
                .PUT("/api/updateIngredient/{ingredientId}", ACCEPT_JSON, ingredientController::updateIngredient)
                .GET("/api/ingredientAllGet/list",recipeController::findAllIngredient)
                .GET("/api/ingredientName", recipeController::findIngredientsByPartialName)  //Get Ingredient Name by filter


                //StockManagement
                .GET("/api/stock", ACCEPT_JSON, ingredientController::getStockManagement)
                .PUT("/api/updateStock", ACCEPT_JSON, ingredientController::updateStockManagement)
                .PUT("/api/updateStockWastage", ACCEPT_JSON, ingredientController::updateWastageManagement)
                .PUT("/api/updateStockUsed", ACCEPT_JSON, ingredientController::updateUsedManagement)


                //Recipe
                .GET("/api/listOfRecipes", recipeController::getAllWithItem)
                .POST("/api/createRecipe",ACCEPT_JSON,recipeController::createRecipe)
                .PUT("/api/updateRecipe/{recipeId}",ACCEPT_JSON,recipeController::updateRecipe)
                .GET("/api/recipe/{recipeId}", recipeController::findRecipeById)
                .PUT("/api/recipe/{recipeId}", recipeController::updateRecipeMaster)
                .GET("/api/searchRecipes", recipeController::searchRecipesWithPagination)
                .GET("/api/recipeDescription", recipeController::findRecipeDescriptionByPartialName)


                //Category
                .GET("/api/category", ACCEPT_JSON, categoryController::getAllCategory)
                .GET("/api/category/{categoryId}", categoryController::getCategoryById)
                .POST("/api/createCategory", ACCEPT_JSON, categoryController::createCategory)
                .PUT("/api/updateCategory/{categoryId}", ACCEPT_JSON, categoryController::updateCategory)
                .DELETE("/api/deleteCategory/{categoryId}", categoryController::deleteCategory)
                .GET("/api/categoryAll/list", recipeController::findAllCategory)


                //Process
                .GET("/api/process", ACCEPT_JSON, processController::getAllProcess)
                .GET("/api/process/{processId}", processController::getProcessById)
                .POST("/api/createProcess", ACCEPT_JSON, processController::createProcess)
                .PUT("/api/updateProcess/{processId}", ACCEPT_JSON, processController::updateProcess)
                .DELETE("/api/deleteProcess/{processId}", processController::deleteProcess)
                .GET("/api/processAll/list", recipeController::findAllProcess)
                .GET("/api/processName", recipeController::findProcessByPartialName)


                //Medium
                .GET("/api/medium", ACCEPT_JSON, mediumController::getAllMedium)
                .GET("/api/medium/{mediumId}", mediumController::getMediumById)
                .POST("/api/createMedium", ACCEPT_JSON, mediumController::createMedium)
                .PUT("/api/updateMedium/{mediumId}", ACCEPT_JSON, mediumController::updateMedium)
                .DELETE("/api/deleteMedium/{mediumId}", mediumController::deleteMedium)
                .GET("/api/mediumAll/list",recipeController::findAllMedium)
                .GET("/api/mediumName", recipeController::findMediumByPartialName)


                //UOM
                .GET("/api/uom", ACCEPT_JSON, uomController::getAllUom)
                .GET("/api/uom/{uomId}", uomController::getUomById)
                .POST("/api/createUom", ACCEPT_JSON, uomController::createUom)
                .PUT("/api/updateUom/{uomId}", ACCEPT_JSON, uomController::updateUom)
                .DELETE("/api/deleteUom/{uomId}", uomController::deleteUom)
                .GET("/api/uomAll/list", recipeController::findAllUom)


                //Order
                .GET("/api/order", ACCEPT_JSON, orderController::getAllOrder)
                .GET("/api/order/{orderId}", orderController::getOrderById)
                .POST("/api/createOrder", ACCEPT_JSON, orderController::createOrder)
                .PUT("/api/updateOrder/{orderId}", ACCEPT_JSON, orderController::updateOrder)
                .DELETE("/api/deleteOrder/{orderId}", orderController::deleteOrder)


                //Vessel
                .GET("/api/vessel", ACCEPT_JSON, vesselController::getAllVessel)
                .GET("/api/vessel/{vesselId}", vesselController::getVesselById)
                .POST("/api/createVessel", ACCEPT_JSON, vesselController::createVessel)
                .PUT("/api/updateVessel/{vesselId}", ACCEPT_JSON, vesselController::updateVessel)
                .DELETE("/api/deleteVessel/{vesselId}", vesselController::deleteVessel)


                //MealItemMapping
                .GET("/api/mealItem", ACCEPT_JSON, mealItemMappingController::getAllMealItem)


                //Grammage
                .GET("/api/gram", ACCEPT_JSON, grammageController::getAllGram)
                .PUT("/api/updateGram", ACCEPT_JSON, grammageController::updateGrammage)


                //CompanyMealCostMapping
                .GET("/api/companyCostList", ACCEPT_JSON, companyController::getAllCompanyCost)
                .PUT("/api/updateCompanyCost", ACCEPT_JSON, companyController::updateCompanyCost)


                //VesselItemMapping
                .GET("/api/vesselItem", ACCEPT_JSON, vesselItemMappingController::getAllVesselItem)
                .PUT("/api/updateVesselItem", ACCEPT_JSON, vesselItemMappingController::updateVesselItem)


                //Indent
                .GET("/api/indent/preparation/{orderDate}/{mealId}", recipeController::findPreparationIndent)
                .GET("/api/indent/hopper/{orderDate}/{mealId}", recipeController::findHopperIndent)
                .GET("/api/indent/hopperItem/{orderDate}/{mealId}", recipeController::findHopperItemIndent)
                .GET("/api/indent/vessel/{orderDate}/{mealId}", recipeController::findVesselIndent)
                .GET("/external/api/indent/vessel/{orderDate}/{mealId}", recipeController::findVesselIndent)
                .GET("/external/api/indent/vessel/any", recipeController::findUnSentVesselIndent)
                .POST("/external/api/http/test", ACCEPT_JSON, recipeController::testHttp)
                .GET("/api/indent/company/{orderDate}/{mealId}", recipeController::getAllCompanyResponses)
                .GET("/api/indent/vesselItem/{orderDate}/{mealId}", recipeController::findVesselItemIndent)
                .build();
    }
}
