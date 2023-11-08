package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.model.OrderMaster;
import com.meteoriqs.foodswing.production.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
@Component
public class OrderController extends BaseController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public Mono<ServerResponse> getAllOrder(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return orderService.getAllOrder(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getOrderById(ServerRequest request) {
        int orderId = Integer.parseInt(request.pathVariable("orderId"));

        return orderService.getOrderById(orderId)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> createOrder(ServerRequest request) {
        Mono<OrderMaster> createOrderRequestMono = request.bodyToMono(OrderMaster.class);

        return createOrderRequestMono
                .flatMap(orderService::createOrder)
                .flatMap(createdOrder -> ServerResponse.status(HttpStatus.CREATED).bodyValue(createdOrder))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("Request body is empty..!!"))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateOrder(ServerRequest request) {
        int orderId = Integer.parseInt(request.pathVariable("orderId"));
        Mono<OrderMaster> updateOrderRequestMono = request.bodyToMono(OrderMaster.class);

        return updateOrderRequestMono.flatMap(updateOrderRequest -> orderService.updateOrder(orderId, updateOrderRequest))
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> deleteOrder(ServerRequest request) {
        int orderId = Integer.parseInt(request.pathVariable("orderId"));

        return orderService.deleteOrder(orderId)
                .flatMap(deletedOrder -> ServerResponse.ok().bodyValue("Order deleted successfully..!!"))
                .onErrorResume(this::handleError);
    }
}
