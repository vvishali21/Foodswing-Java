package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.model.MealCategoryMappingRequest;
import com.meteoriqs.foodswing.production.service.MealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class MealController extends BaseController {

    private final MealService mealService;

    @Autowired
    public MealController(MealService mealService) {
        this.mealService = mealService;
    }


    public Mono<ServerResponse> getAllMeal(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return mealService.getAllMeal(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getMealById(ServerRequest request) {
        int mealId = Integer.parseInt(request.pathVariable("id"));

        return mealService.getMealById(mealId)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> createMeal(ServerRequest request) {
        Mono<MealCategoryMappingRequest> createMealRequestMono = request.bodyToMono(MealCategoryMappingRequest.class);

        return createMealRequestMono
                .flatMap(mealService::createMeal)
                .flatMap(createdMeal -> ServerResponse.status(HttpStatus.CREATED).bodyValue(createdMeal))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("Request body is empty..!!"))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateMeal(ServerRequest request) {
        int mealId = Integer.parseInt(request.pathVariable("id"));
        Mono<MealCategoryMappingRequest> updateMealRequestMono = request.bodyToMono(MealCategoryMappingRequest.class);

        return updateMealRequestMono.flatMap(updateMealRequest -> mealService.updateMeal(mealId, updateMealRequest))
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> deleteMeal(ServerRequest request) {
        int mealId = Integer.parseInt(request.pathVariable("id"));

        return mealService.deleteMeal(mealId)
                .flatMap(deletedMeal -> ServerResponse.ok().bodyValue("Meal deleted successfully..!!"))
                .onErrorResume(this::handleError);
    }

}

