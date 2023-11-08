package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.model.VesselItemMappingResponse;
import com.meteoriqs.foodswing.production.service.VesselService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class VesselController extends BaseController{

    private final VesselService vesselService;

    @Autowired
    public VesselController(VesselService vesselService) {
        this.vesselService = vesselService;
    }

    public Mono<ServerResponse> getAllVessel(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return vesselService.getAllVessel(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getVesselById(ServerRequest request) {
        int vesselId = Integer.parseInt(request.pathVariable("vesselId"));

        return vesselService.getVesselById(vesselId)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> createVessel(ServerRequest request) {
        Mono<VesselItemMappingResponse> createVesselRequestMono = request.bodyToMono(VesselItemMappingResponse.class);

        return createVesselRequestMono
                .flatMap(vesselService::createVessel)
                .flatMap(createdVessel -> ServerResponse.status(HttpStatus.CREATED).bodyValue(createdVessel))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("Request body is empty..!!"))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateVessel(ServerRequest request) {
        int vesselId = Integer.parseInt(request.pathVariable("vesselId"));
        Mono<VesselItemMappingResponse> updateVesselRequestMono = request.bodyToMono(VesselItemMappingResponse.class);

        return updateVesselRequestMono.flatMap(updateVesselRequest -> vesselService.updateVessel(vesselId, updateVesselRequest))
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> deleteVessel(ServerRequest request) {
        int vesselId = Integer.parseInt(request.pathVariable("vesselId"));

        return vesselService.deleteVessel(vesselId)
                .flatMap(deletedVessel -> ServerResponse.ok().bodyValue("Vessel deleted successfully..!!"))
                .onErrorResume(this::handleError);
    }
}
