package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.model.ItemMealGramResponse;
import com.meteoriqs.foodswing.production.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ItemController extends BaseController {

    private final ItemService itemService;

    @Autowired
    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    public Mono<ServerResponse> getAllItems(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return itemService.getAllItems(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getItemById(ServerRequest request) {
        int itemId = Integer.parseInt(request.pathVariable("id"));

        return itemService.getItemById(itemId)
                .flatMap(itemMealGramResponse -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(itemMealGramResponse))
                .onErrorResume(this::handleError);
    }


    public Mono<ServerResponse> createItem(ServerRequest request) {
        Mono<ItemMealGramResponse> createItemRequestMono = request.bodyToMono(ItemMealGramResponse.class);

        return createItemRequestMono
                .flatMap(itemService::createItem)
                .flatMap(createdItem -> ServerResponse.status(HttpStatus.CREATED).bodyValue(createdItem))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("Request body is empty..!!"))
                .onErrorResume(this::handleError);
    }


    public Mono<ServerResponse> updateItem(ServerRequest request) {
        int itemId = Integer.parseInt(request.pathVariable("id"));
        Mono<ItemMealGramResponse> updateItemRequestMono = request.bodyToMono(ItemMealGramResponse.class);

        return updateItemRequestMono.flatMap(itemMealGramResponse -> itemService.updateItem(itemId, itemMealGramResponse))
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> deleteItem(ServerRequest request) {
        int itemId = Integer.parseInt(request.pathVariable("id"));

        return itemService.deleteItem(itemId)
                .flatMap(deletedItem -> ServerResponse.ok().bodyValue("Item deleted successfully..!!"))
                .onErrorResume(this::handleError);
    }

}
