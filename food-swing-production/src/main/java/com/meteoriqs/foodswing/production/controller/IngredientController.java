package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.model.Ingredients;
import com.meteoriqs.foodswing.data.model.Status;
import com.meteoriqs.foodswing.production.service.IngredientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.data.domain.Pageable;


@Component
public class IngredientController extends BaseController {

  private final IngredientService ingredientService;

  @Autowired
  public IngredientController(IngredientService ingredientService){
      this.ingredientService = ingredientService;
  }

    public Mono<ServerResponse> getAllIngredients(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);
        return ingredientService.getAllIngredients(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }
    public Mono<ServerResponse> getAllIngredientsSample(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);
        return ingredientService.getAllIngredients(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getIngredientById(ServerRequest request) {
        int ingredientId = Integer.parseInt(request.pathVariable("ingredientId"));

        return ingredientService.getIngredientById(ingredientId)
                .flatMap(this::okResponse)
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> deleteIngredient(ServerRequest request) {
        int ingredientId = Integer.parseInt(request.pathVariable("ingredientId"));

        return ingredientService.deleteIngredientById(ingredientId)
                .flatMap(response -> {
                    if (response.getStatus().getCode() == 201) {
                        return ServerResponse.ok().bodyValue(response);
                    } else if (response.getStatus().getCode() == 404) {
                        return ServerResponse.notFound().build();
                    } else {
                        return ServerResponse.status(response.getStatus().getCode()).bodyValue(response);
                    }
                });
    }

    public Mono<ServerResponse> createIngredient(ServerRequest request) {
        Mono<Ingredients> createIngredientRequestMono = request.bodyToMono(Ingredients.class);

        return createIngredientRequestMono
                .flatMap(ingredientService::createIngredient)
                .flatMap(createdIngredient -> ServerResponse.status(HttpStatus.CREATED).bodyValue(createdIngredient))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("Request body is empty..!!"))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateIngredient(ServerRequest request) {
        int ingredientId = Integer.parseInt(request.pathVariable("ingredientId"));
        Mono<Ingredients> ingredientMono = request.bodyToMono(Ingredients.class);

        return ingredientMono
                .flatMap(ingredient -> ingredientService.updateIngredient(ingredientId, ingredient))
                .flatMap(this::okResponse)
                .switchIfEmpty(ServerResponse.badRequest().bodyValue(
                        new CustomResponse<>(new Status(500,"Input Fields are Required"),null)
                ))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getStockManagement(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);
        return ingredientService.getAllIngredientsStock(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }


    public Mono<ServerResponse> updateStockManagement(ServerRequest request) {
        Flux<Ingredients> ingredientFlux = request.bodyToFlux(Ingredients.class);

        return ingredientFlux.collectList()
                .flatMap(ingredients -> ingredientService.updateIngredientStock(ingredients)
                        .flatMap(this::okResponse)
                )
                .switchIfEmpty(ServerResponse.badRequest().bodyValue(
                        new CustomResponse<>(new Status(500, "Input Fields are Required"), null)
                ))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateWastageManagement(ServerRequest request) {
        Flux<Ingredients> ingredientFlux = request.bodyToFlux(Ingredients.class);

        return ingredientFlux.collectList()
                .flatMap(ingredients -> ingredientService.updateIngredientWastage(ingredients)
                        .flatMap(this::okResponse)
                )
                .switchIfEmpty(ServerResponse.badRequest().bodyValue(
                        new CustomResponse<>(new Status(500, "Input Fields are Required"), null)
                ))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateUsedManagement(ServerRequest request) {
        Flux<Ingredients> ingredientFlux = request.bodyToFlux(Ingredients.class);

        return ingredientFlux.collectList()
                .flatMap(ingredients -> ingredientService.updateIngredientUsed(ingredients)
                        .flatMap(this::okResponse)
                )
                .switchIfEmpty(ServerResponse.badRequest().bodyValue(
                        new CustomResponse<>(new Status(500, "Input Fields are Required"), null)
                ))
                .onErrorResume(this::handleError);
    }


}
