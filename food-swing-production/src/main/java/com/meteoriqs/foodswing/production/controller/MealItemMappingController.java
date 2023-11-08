package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.production.service.MealItemMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
@Component
public class MealItemMappingController extends BaseController{

    private final MealItemMappingService mealItemMappingService;

    @Autowired
    public MealItemMappingController(MealItemMappingService mealItemMappingService) {
        this.mealItemMappingService = mealItemMappingService;
    }

    public Mono<ServerResponse> getAllMealItem(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return mealItemMappingService.getAllMealItem(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }
}
