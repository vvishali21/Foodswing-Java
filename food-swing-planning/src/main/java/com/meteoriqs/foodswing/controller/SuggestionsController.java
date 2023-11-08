package com.meteoriqs.foodswing.controller;

import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.data.repository.OrderMasterRepository;
import com.meteoriqs.foodswing.model.MenuSuggestionResponse;
import com.meteoriqs.foodswing.model.OrderPlanningSaveResponse;
import com.meteoriqs.foodswing.service.SuggestionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SuggestionsController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(SuggestionsService.class);

    private final SuggestionsService suggestionsService;
    private final OrderMasterRepository orderMasterRepository;

    @Autowired
    public SuggestionsController(SuggestionsService suggestionsService,
                                 OrderMasterRepository orderMasterRepository) {
        this.suggestionsService = suggestionsService;
        this.orderMasterRepository = orderMasterRepository;
    }

    public Mono<ServerResponse> getOrders(ServerRequest request) {
        String mealId = request.queryParam("mealId").orElse(null);
        String orderDate = request.queryParam("orderDate").orElse(null);
        Flux<OrderMaster> orders = suggestionsService.getOrders(Integer.parseInt(mealId), orderDate);
        return orders.collectList().flatMap(orderList -> {
            CustomResponse<List<OrderMaster>> response = new CustomResponse<>();
            response.setStatus(new Status(200, "Success"));
            response.setData(orderList);
            return ServerResponse.ok().bodyValue(response);
        });
    }

    public Mono<ServerResponse> getOrdersBySuggestionId(ServerRequest request) {
        String mealId = request.queryParam("mealId").orElse(null);
        String orderDate = request.queryParam("orderDate").orElse(null);
        String suggestionId = request.queryParam("mealSuggestionId").orElse(null);
        Flux<OrderMaster> orders = suggestionsService.getOrdersByMealSuggestionId(Integer.parseInt(mealId), orderDate, Integer.parseInt(suggestionId));
        return orders.collectList().flatMap(orderList -> {
            CustomResponse<List<OrderMaster>> response = new CustomResponse<>();
            response.setStatus(new Status(200, "Success"));
            response.setData(orderList);
            return ServerResponse.ok().bodyValue(response);
        });
    }


    public Mono<ServerResponse> editMealSuggestion(ServerRequest request) {
        try {
            int mealId = Integer.parseInt(request.queryParam("mealId").orElseThrow(() -> new NumberFormatException("Missing mealId")));
            String orderDate = request.queryParam("orderDate").orElse(null);
            List<Integer> companyIdList = Arrays.stream(request.pathVariable("companyIds").split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            int mealCount = Integer.parseInt(request.queryParam("mealCount").orElseThrow(() -> new NumberFormatException("Missing mealCount")));

            return request.bodyToFlux(MenuSuggestionResponse.class)
                    .flatMap(payload -> suggestionsService.editMealSuggestion(payload, mealId, orderDate, companyIdList, mealCount))
                    .collectList()
                    .flatMap(responses -> {
                        boolean allSuccessful = responses.stream().allMatch(response -> response.getStatus().getCode() == 200);
                        if (allSuccessful) {
                            return ServerResponse.ok().bodyValue(responses);
                        } else {
                            return ServerResponse.badRequest().bodyValue(responses);
                        }
                    })
                    .onErrorResume(error -> ServerResponse.badRequest()
                            .bodyValue(new CustomResponse<>(new Status(400, "Error"), "Error processing the request.")));
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest()
                    .bodyValue(new CustomResponse<>(new Status(400, "Bad Request"), "Invalid parameter format."));
        }
    }

    public Mono<ServerResponse> processPlans(ServerRequest request) {
        return request.bodyToMono(PlanUpdateandDeletePayload.class)
                .flatMap(suggestionsService::processPlans)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> getListPlan(ServerRequest request) {
        String mealId = request.queryParam("mealId").orElse(null);
        String orderDate = request.queryParam("orderDate").orElse(null);
        Flux<PlanList> orders = suggestionsService.getPlanList(Integer.parseInt(mealId), orderDate);
        return orders.collectList().flatMap(orderList -> {
            CustomResponse<List<PlanList>> response = new CustomResponse<>();
            response.setStatus(new Status(200, "Success"));
            response.setData(orderList);
            return ServerResponse.ok().bodyValue(response);
        });
    }


    public Mono<ServerResponse> completePlan(ServerRequest request) {
        return request.bodyToMono(MealSuggestionRequest.class)
                .flatMap(mealSuggestionRequest -> {
                    int planId = Integer.parseInt(request.pathVariable("planId"));
                    int mealSuggestionId = mealSuggestionRequest.getMealSuggestionId();

                    return suggestionsService.completePlan(planId, mealSuggestionId)
                            .flatMap(result -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromValue(new CustomResponse<>(new Status(200, "Plan completed successfully"), null)))
                            )
                            .switchIfEmpty(ServerResponse.notFound().build())
                            .onErrorResume(error -> ServerResponse.badRequest()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromValue(new CustomResponse<>(new Status(400, "Error"), "Error processing the request."))
                                    ));
                });
    }


    public Mono<ServerResponse> removePlan(ServerRequest request) {
        int mealSuggestionId = Integer.parseInt(request.pathVariable("mealSuggestionId"));

        return orderMasterRepository.updateMealSuggestionId(mealSuggestionId)
                .flatMap(updatedRows -> {
                    if (updatedRows > 0) {
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(new CustomResponse<>(new Status(200, "Plan Removed successfully"), null));
                    } else {
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(new CustomResponse<>(new Status(404, "Order not found"), null));
                    }
                })
                .doOnError(throwable -> log.error("Error occurred: {}", throwable.getMessage()))
                .onErrorResume(throwable -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new CustomResponse<>(new Status(500, "Internal Server Error"), null)));
    }


    public Mono<ServerResponse> savePlanning(ServerRequest request) {
        Mono<OrderPlanningSaveResponse> payloadMono = request.bodyToMono(OrderPlanningSaveResponse.class);

        return payloadMono
                .flatMap(payload -> suggestionsService.savePlanning(payload.getMealSuggestionId(), payload.getOrderIds())
                        .then(ServerResponse.ok().bodyValue(new CustomResponse<>(new Status(200, "Plan saved successfully"), null))))
                .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND).bodyValue(getNotFoundResponses()))
                .onErrorResume(error -> ServerResponse.status(HttpStatus.BAD_REQUEST).bodyValue(new CustomResponse<>(new Status(400, "Error"), "Error processing the request.")));
    }

    private CustomResponse<String> getNotFoundResponses() {
        return new CustomResponse<>(new Status(404, "MealSuggestion not found"), null);
    }


    public Mono<ServerResponse> validatePlan(ServerRequest request) {
        int mealSuggestionId = Integer.parseInt(request.pathVariable("mealSuggestionId"));

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(suggestionsService.combineResponses(mealSuggestionId), VesselItemMapping.class);
    }


    public Mono<ServerResponse> approvePlanning(ServerRequest request) {
        return request.bodyToMono(VesselPlanRequest.class)
                .flatMap(payload -> suggestionsService.approvePlanning(payload)
                        .then(ServerResponse.ok()
                                .bodyValue(new CustomResponse<>(new Status(200, "Plan approved successfully"), null)))
                )
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(error -> ServerResponse.badRequest()
                        .bodyValue(new CustomResponse<>(new Status(400, "Error"), "Error processing the request."))
                );
    }


    public Mono<ServerResponse> getMealSuggestions(ServerRequest request) {
        int mealId = Integer.parseInt(request.pathVariable("mealId"));
        String orderDate = request.pathVariable("orderDate");
        String companyIds = request.pathVariable("companyIds");
        int mealCount = Integer.parseInt(request.pathVariable("mealCount"));
        return suggestionsService.getMealSuggestions(mealId, orderDate, companyIds, mealCount)
                .flatMap(itemMappings -> ServerResponse.ok().bodyValue(itemMappings))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> getMealSuggestionDetailsBySuggestionId(ServerRequest request) {
        int mealSuggestionId = Integer.parseInt(request.pathVariable("id"));

        return suggestionsService.getMealSuggestionDetailsBySuggestionId(mealSuggestionId)
                .collectList()
                .flatMap(suggestions -> {
                    if (suggestions.isEmpty()) {
                        return okResponse(getNotFoundResponse());
                    } else {
                        return okResponse(new CustomResponse<>(new Status(200, "Success"), suggestions));
                    }
                })
                .onErrorResume(this::handleError);
    }

    private CustomResponse<MealSuggestionDetails> getNotFoundResponse() {
        return new CustomResponse<>(new Status(404, "MenuDetail not found"), null);
    }

    public Mono<ServerResponse> getIngredientSummary(ServerRequest request) {
        int mealSuggestionId = Integer.parseInt(request.pathVariable("id"));
        int mealCount = Integer.parseInt(request.pathVariable("mealCount"));
        return suggestionsService.getMealSuggestionIngredientSummary(mealSuggestionId, mealCount)
                .flatMap(summary -> okResponse(new CustomResponse<>(new Status(200, "Success"), summary)))
                .switchIfEmpty(okResponse(getNotFoundResponse()))
                .onErrorResume(this::handleError);
    }


}
