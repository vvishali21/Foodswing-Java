package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.entity.MediumMaster;
import com.meteoriqs.foodswing.production.service.MediumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class MediumController extends BaseController{

    private final MediumService mediumService;

    @Autowired
    public MediumController(MediumService mediumService) {
        this.mediumService = mediumService;
    }

    public Mono<ServerResponse> getAllMedium(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return mediumService.getAllMedium(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getMediumById(ServerRequest request) {
        int mediumId = Integer.parseInt(request.pathVariable("mediumId"));

        return mediumService.getMediumById(mediumId)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> createMedium(ServerRequest request) {
        Mono<MediumMaster> createMediumRequestMono = request.bodyToMono(MediumMaster.class);

        return createMediumRequestMono
                .flatMap(mediumService::createMedium)
                .flatMap(createdMedium -> ServerResponse.status(HttpStatus.CREATED).bodyValue(createdMedium))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("Request body is empty..!!"))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateMedium(ServerRequest request) {
        int mediumId = Integer.parseInt(request.pathVariable("mediumId"));
        Mono<MediumMaster> updateMediumRequestMono = request.bodyToMono(MediumMaster.class);

        return updateMediumRequestMono.flatMap(updateMediumRequest -> mediumService.updateMedium
                        (mediumId, updateMediumRequest))
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> deleteMedium(ServerRequest request) {
        int mediumId = Integer.parseInt(request.pathVariable("mediumId"));

        return mediumService.deleteMedium(mediumId)
                .flatMap(deletedMedium -> ServerResponse.ok().bodyValue("Medium deleted successfully..!!"))
                .onErrorResume(this::handleError);
    }
}
