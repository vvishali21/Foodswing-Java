package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.model.Grammage;
import com.meteoriqs.foodswing.production.service.GrammageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GrammageController extends BaseController{

    private final GrammageService grammageService;

    @Autowired
    public GrammageController(GrammageService grammageService) {
        this.grammageService = grammageService;
    }

    public Mono<ServerResponse> getAllGram(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return grammageService.getAllGram(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateGrammage(ServerRequest request) {
        Flux<Grammage> updateGrammageRequestFlux = request.bodyToFlux(Grammage.class);

        return grammageService.updateGrammage(updateGrammageRequestFlux)
                .flatMap(customResponse -> {
                    if (customResponse.getStatus().getCode() == 200) {
                        return ServerResponse.ok().bodyValue(customResponse);
                    } else {
                        return ServerResponse.ok().bodyValue(customResponse);
                    }
                })
                .onErrorResume(e -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }


}
