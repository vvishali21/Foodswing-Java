package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.entity.UpdateRecipeRequest;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.production.service.RecipeService;
import lombok.extern.log4j.Log4j2;
import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;


@Component
@Log4j2
public class RecipeController extends BaseController {

    private final RecipeService recipeService;

    private final MemcachedClient memcachedClient;

    @Autowired
    public RecipeController(RecipeService recipeService, MemcachedClient memcachedClient){
        this.recipeService = recipeService;
        this.memcachedClient = memcachedClient;
    }

    public Mono<ServerResponse> getAllWithItem(ServerRequest request){
        Pageable pageable = getPageableInfo(request);
        Mono<RecipeResponse> responseMono = recipeService.getAllRecipeWithItem(pageable);
        return responseMono.flatMap(response->ServerResponse.ok().bodyValue(response));
    }


    public Mono<ServerResponse> findPreparationIndent(ServerRequest request) {
        int mealId = Integer.parseInt(request.pathVariable("mealId"));
        String orderDate = request.pathVariable("orderDate");

        Flux<PreparationIndentResponse> results = recipeService.getPreparationIndent(orderDate, mealId);

        return results.collectList()
                .flatMap(customResultDTOList -> {
                    if (!customResultDTOList.isEmpty()) {
                        Status status = new Status(200, "Success");
                        CustomResponse<List<PreparationIndentResponse>> customResponse = new CustomResponse<>(status, customResultDTOList);
                        return ServerResponse.ok().bodyValue(customResponse);
                    } else {
                        Status status = new Status(404, "No results found");
                        CustomResponse<String> customResponse = new CustomResponse<>(status, null);
                        return ServerResponse.ok().bodyValue(customResponse);
                    }
                });
    }

    public Mono<ServerResponse> findHopperIndent(ServerRequest request) {
        int mealId = Integer.parseInt(request.pathVariable("mealId"));
        String orderDate = request.pathVariable("orderDate");

        Flux<HopperIndentResponse> results = recipeService.getHopperIndent(orderDate, mealId);

        Flux<HopperCategoriesResponse> categories = results
                .groupBy(HopperIndentResponse::getItemName)
                .flatMap(group -> group.collectList()
                        .map(items -> new HopperCategoriesResponse(group.key(), items)));

        return categories.collectList()
                .flatMap(categoriesList -> {
                    if (!categoriesList.isEmpty()) {
                        Map<String, List<HopperCategoriesResponse>> data = new HashMap<>();
                        data.put("categories", categoriesList);
                        Status status = new Status(200, "Success");
                        CustomResponse<Map<String, List<HopperCategoriesResponse>>> customResponse = new CustomResponse<>(status, data);
                        return ServerResponse.ok().bodyValue(customResponse);
                    } else {
                        Status status = new Status(404, "No results found");
                        CustomResponse<String> customResponse = new CustomResponse<>(status, null);
                        return ServerResponse.ok().bodyValue(customResponse);
                    }
                });
    }

    public Mono<ServerResponse> findHopperItemIndent(ServerRequest request) {
        int mealId = Integer.parseInt(request.pathVariable("mealId"));
        String orderDate = request.pathVariable("orderDate");

        Flux<HopperIndentItemResponse> results = recipeService.getHopperItemIndent(orderDate, mealId);

        Flux<HopperCategoriesItemResponse> categories = results
                .groupBy(HopperIndentItemResponse::getItemName)
                .flatMap(group -> group.collectList()
                        .map(items -> new HopperCategoriesItemResponse(group.key(), items)));

        return categories.collectList()
                .flatMap(categoriesList -> {
                    if (!categoriesList.isEmpty()) {
                        Map<String, List<HopperCategoriesItemResponse>> data = new HashMap<>();
                        data.put("categories", categoriesList);
                        Status status = new Status(200, "Success");
                        CustomResponse<Map<String, List<HopperCategoriesItemResponse>>> customResponse = new CustomResponse<>(status, data);
                        return ServerResponse.ok().bodyValue(customResponse);
                    } else {
                        Status status = new Status(404, "No results found");
                        CustomResponse<String> customResponse = new CustomResponse<>(status, null);
                        return ServerResponse.ok().bodyValue(customResponse);
                    }
                });
    }


    public Mono<ServerResponse> findVesselIndent(ServerRequest request) {
        int mealId = Integer.parseInt(request.pathVariable("mealId"));
        String orderDate = request.pathVariable("orderDate");

        Flux<VesselTransformedIndentResponse> results = recipeService.getVesselIndent(orderDate, mealId);

        return results.collectList()
                .flatMap(transformedVesselResults -> {
                    if (!transformedVesselResults.isEmpty()) {
                        // Sort the results by stage and then by item name
                        transformedVesselResults.sort(Comparator.comparing(result -> ((VesselTransformedIndentResponse) result).getStage().getItemName())
                                .thenComparing(result -> ((VesselTransformedIndentResponse) result).getStage().getStage()));

                        Status status = new Status(200, "Success");
                        CustomResponse<List<VesselTransformedIndentResponse>> customResponse = new CustomResponse<>(status, transformedVesselResults);
                        return ServerResponse.ok().body(BodyInserters.fromValue(customResponse));
                    } else {
                        Status status = new Status(404, "No results found");
                        CustomResponse<Void> customResponse = new CustomResponse<>(status, null);
                        return ServerResponse.ok().body(BodyInserters.fromValue(customResponse));
                    }
                });
    }


    public Mono<ServerResponse> findVesselItemIndent(ServerRequest request) {
        int mealId = Integer.parseInt(request.pathVariable("mealId"));
        String orderDate = request.pathVariable("orderDate");

        Flux<VesselItemTransformedIndentResponse> results = recipeService.getVesselItemIndent(orderDate, mealId);

        return results.collectList()
                .flatMap(transformedVesselResults -> {
                    if (!transformedVesselResults.isEmpty()) {
                        // Sort the results by stage and then by item name
                        transformedVesselResults.sort(Comparator.comparing(result -> ((VesselItemTransformedIndentResponse) result).getStage().getItemName())
                                .thenComparing(result -> ((VesselItemTransformedIndentResponse) result).getStage().getStage()));

                        Status status = new Status(200, "Success");
                        CustomResponse<List<VesselItemTransformedIndentResponse>> customResponse = new CustomResponse<>(status, transformedVesselResults);
                        return ServerResponse.ok().body(BodyInserters.fromValue(customResponse));
                    } else {
                        Status status = new Status(404, "No results found");
                        CustomResponse<Void> customResponse = new CustomResponse<>(status, null);
                        return ServerResponse.ok().body(BodyInserters.fromValue(customResponse));
                    }
                });
    }

    public Mono<ServerResponse> testHttp(ServerRequest request) {
        int itemId = 1;
        log.info("Cache :: item 1 :: {}", memcachedClient.get("itemNames-"+itemId));
        return request
                .bodyToMono(String.class)
                .flatMap(payload -> {
                    log.info("HTTP Request Received with payload: {}", payload);
                    return ServerResponse.ok().bodyValue(Map.of("ResponseFrom", "FoodSwing ERP",
                            "CreatedBy", "Admin",
                            "CreatedOn", new Date()));
                });
    }

    public Mono<ServerResponse> findUnSentVesselIndent(ServerRequest request) {
        Flux<VesselTransformedIndentResponse> results = recipeService.getUnSentVesselIndent();
        return results.collectList()
                .flatMap(transformedVesselResults -> {
                    if (!transformedVesselResults.isEmpty()) {
                        // Sort the results by stage and then by item name
                        transformedVesselResults.sort(Comparator.comparing(result -> ((VesselTransformedIndentResponse) result).getStage().getItemName())
                                .thenComparing(result -> ((VesselTransformedIndentResponse) result).getStage().getStage()));

                        Status status = new Status(200, "Success");
                        CustomResponse<List<VesselTransformedIndentResponse>> customResponse = new CustomResponse<>(status, transformedVesselResults);
                        return ServerResponse.ok().body(BodyInserters.fromValue(customResponse));
                    } else {
                        Status status = new Status(404, "No results found");
                        CustomResponse<Void> customResponse = new CustomResponse<>(status, null);
                        return ServerResponse.ok().body(BodyInserters.fromValue(customResponse));
                    }
                });
    }


    public Mono<ServerResponse> getAllCompanyResponses(ServerRequest request) {
        int mealId = Integer.parseInt(request.pathVariable("mealId"));
        String orderDate = request.pathVariable("orderDate");

        Flux<CompanyOrderResponse> results = recipeService.getCompanyOrders(orderDate, mealId);

        return results.collectList()
                .flatMap(customResultDTOList -> {
                    if (!customResultDTOList.isEmpty()) {
                        Status status = new Status(200, "Success");
                        CustomResponse<List<CompanyOrderResponse>> customResponse = new CustomResponse<>(status, customResultDTOList);
                        return ServerResponse.ok().bodyValue(customResponse);
                    } else {
                        Status status = new Status(404, "No results found");
                        CustomResponse<String> customResponse = new CustomResponse<>(status, null);
                        return ServerResponse.ok().bodyValue(customResponse);
                    }
                });
    }


    public Mono<ServerResponse> createRecipe(ServerRequest request) {
        return request.bodyToMono(RecipeMasterDetailsRequest.class)
                .flatMap(recipeService::createRecipeNew)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateRecipe(ServerRequest request){
        int recipeId = Integer.parseInt(request.pathVariable("recipeId"));
        Mono<RecipeRequest> recipeMono = request.bodyToMono(RecipeRequest.class);

        return recipeMono
                .flatMap(recipeRequest -> recipeService.updateRecipe(recipeId,recipeRequest))
                .flatMap(this::okResponse)
                .switchIfEmpty(ServerResponse.badRequest().bodyValue(
                        new CustomResponse<>(new Status(500,"Input Fields are Required"),null)
                ))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateRecipeMaster(ServerRequest request) {
        int recipeId = Integer.parseInt(request.pathVariable("recipeId"));
        Mono<UpdateRecipeRequest> updateRecipeRequestMono = request.bodyToMono(UpdateRecipeRequest.class);

        return updateRecipeRequestMono.flatMap(updateRecipeRequest ->
                recipeService.updateRecipeMaster(recipeId, updateRecipeRequest) // Pass the entire updateRecipeRequest
                        .flatMap(updatedRecipe ->
                                ServerResponse.ok().bodyValue(updatedRecipe))
                        .switchIfEmpty(ServerResponse.notFound().build())
                        .onErrorResume(throwable ->
                                ServerResponse.badRequest().bodyValue("Error updating recipe master: " + throwable.getMessage())
                        )
        );
    }

    public Mono<ServerResponse> findIngredientsByPartialName(ServerRequest request) {
        String partialName = request.queryParam("partialName").orElse("");
        return recipeService.getIngredientsResponse(partialName)
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findProcessByPartialName(ServerRequest request) {
        String partialName = request.queryParam("partialName").orElse("");
        return recipeService.getProcessByPartialName(partialName)
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findAllProcess(ServerRequest request){
        return recipeService.getAllProcess()
                .flatMap(response-> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findAllMedium(ServerRequest request){
        return recipeService.getAllMedium()
                .flatMap(response-> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findAllIngredient(ServerRequest request){
        return recipeService.getAllIngredientValue()
                .flatMap(response-> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findAllUom(ServerRequest request){
        return recipeService.getAllUomValue()
                .flatMap(response-> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findAllMeal(ServerRequest request){
        return recipeService.getAllMealValue()
                .flatMap(response-> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findAllCategory(ServerRequest request){
        return recipeService.getAllCategoryValue()
                .flatMap(response-> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findAllCompany(ServerRequest request){
        return recipeService.getAllCompanyValue()
                .flatMap(response-> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findAllItem(ServerRequest request){
        return recipeService.getAllItemValue()
                .flatMap(response-> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findMediumByPartialName(ServerRequest request) {
        String partialName = request.queryParam("partialName").orElse("");
        return recipeService.getMediumByPartialName(partialName)
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findItemByPartialName(ServerRequest request) {
        String partialName = request.queryParam("partialName").orElse("");
        return recipeService.getItemByPartialName(partialName)
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }


    public Mono<ServerResponse> searchRecipesWithPagination(ServerRequest request) {
        String partialName = request.queryParam("partialName").orElse("");
        Pageable pageable = getPageableInfo(request);
        Mono<RecipeResponse> responseMono = recipeService.searchRecipesWithPagination(pageable, partialName);

        return responseMono.flatMap(response->ServerResponse.ok().bodyValue(response));
    }


    public Mono<ServerResponse> findRecipeDescriptionByPartialName(ServerRequest request) {
        String partialName = request.queryParam("partialName").orElse("");
        return recipeService.getRecipeDescriptionByPartialName(partialName)
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findRecipeById(ServerRequest request) {
        int recipeId = Integer.parseInt(request.pathVariable("recipeId"));
        return recipeService.findRecipeById(recipeId)
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }
}
