package com.meteoriqs.foodswing.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

public class BaseController {

    protected Pageable getPageableInfo(ServerRequest request) {
        int page = request.queryParam("page").map(Integer::parseInt).orElse(1);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(10);
        return PageRequest.of(page - 1, size);
    }

    protected <T> Mono<ServerResponse> okResponse(T responseBody) {
        return ServerResponse.ok().bodyValue(responseBody);
    }

    protected Mono<ServerResponse> handleError(Throwable throwable) {
        if (throwable instanceof ResponseStatusException) {
            ResponseStatusException ex = (ResponseStatusException) throwable;
            return ServerResponse.status(ex.getStatusCode()).bodyValue(ex.getReason());
        } else {
            return ServerResponse.badRequest().bodyValue("Request failed");
        }
    }

}
