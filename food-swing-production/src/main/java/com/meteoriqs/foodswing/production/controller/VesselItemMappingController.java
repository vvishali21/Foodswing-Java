package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.model.VesselItemMapping;
import com.meteoriqs.foodswing.production.service.VesselItemMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class VesselItemMappingController extends BaseController {

    private final VesselItemMappingService vesselItemMappingService;

    @Autowired
    public VesselItemMappingController(VesselItemMappingService vesselItemMappingService) {
        this.vesselItemMappingService = vesselItemMappingService;
    }

    public Mono<ServerResponse> getAllVesselItem(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return vesselItemMappingService.getAllVesselItem(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }


    public Mono<ServerResponse> updateVesselItem(ServerRequest request) {
        Flux<VesselItemMapping> updateVesselRequestFlux = request.bodyToFlux(VesselItemMapping.class);

        return vesselItemMappingService.updateVesselItem(updateVesselRequestFlux)
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
