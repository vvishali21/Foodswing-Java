package com.meteoriqs.foodswing.config;

import com.meteoriqs.foodswing.controller.SuggestionsController;
import com.meteoriqs.foodswing.controller.TestController;
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
    public RouterFunction<ServerResponse> allRoutes(SuggestionsController suggestionsController,
                                                    TestController testController) {
        return RouterFunctions.route()
                .GET("/api/company", testController::getAllCompany)


                //MealSuggestion
                .GET("/api/suggestions/list/{orderDate}/{mealId}/{companyIds}/{mealCount}", ACCEPT_JSON,
                        suggestionsController::getMealSuggestions)
                .GET("/api/mealSuggestionDetails/{id}", ACCEPT_JSON,
                        suggestionsController::getMealSuggestionDetailsBySuggestionId)
                .GET("/api/ingredientSummary/mealSuggestions/{id}/{mealCount}", ACCEPT_JSON,
                        suggestionsController::getIngredientSummary)


                //Order
                .GET("/api/orders", suggestionsController::getOrders)
                .POST("/api/planning/save", ACCEPT_JSON, suggestionsController::savePlanning)
                .GET("/api/orders/suggestionGetById",suggestionsController::getOrdersBySuggestionId)
                .PUT("/api/processPlans",ACCEPT_JSON,suggestionsController::processPlans)
                .POST("/api/editMealSuggestion/{companyIds}", ACCEPT_JSON, suggestionsController::editMealSuggestion)


                //Plan
                .GET("/api/planList", suggestionsController::getListPlan)
                .PUT("/api/updatePlan/{mealSuggestionId}", ACCEPT_JSON, suggestionsController::removePlan)
                .GET("/api/validatePlan/{mealSuggestionId}", ACCEPT_JSON, suggestionsController::validatePlan)
                .POST("/api/plan/approve", ACCEPT_JSON, suggestionsController::approvePlanning)
                .PUT("/api/plan/complete/{planId}", ACCEPT_JSON, suggestionsController::completePlan)

                .build();
    }
}
