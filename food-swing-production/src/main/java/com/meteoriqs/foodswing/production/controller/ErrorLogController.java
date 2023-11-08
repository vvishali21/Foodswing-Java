package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.entity.ErrorLogEntity;
import com.meteoriqs.foodswing.production.service.ErrorLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ErrorLogController extends BaseController {
    private final ErrorLogService errorLogService;

    public ErrorLogController(ErrorLogService errorLogService){
        this.errorLogService = errorLogService;
    }

    //Todo Want to check ErrorLock is working correctly
    public Mono<ServerResponse> createErrorLog(ServerRequest request){
        return request.bodyToMono(ErrorLogEntity.class)
                .flatMap(errorLogService::createErrorLog)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }
}
