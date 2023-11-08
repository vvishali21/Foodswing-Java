package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.production.record.UserRecord;
import com.meteoriqs.foodswing.production.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    public Mono<ServerResponse> createUser(ServerRequest request) {
        return request.bodyToMono(UserRecord.class)
                .flatMap(userRecord -> ServerResponse.ok().bodyValue(userService.createUser(userRecord)))
                .onErrorResume(throwable -> ServerResponse.badRequest().build());
    }
}
