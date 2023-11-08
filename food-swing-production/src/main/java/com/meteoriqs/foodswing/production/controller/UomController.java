package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.model.UomMasterEntity;
import com.meteoriqs.foodswing.production.service.UomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class UomController extends BaseController{

    private final UomService uomService;

    @Autowired
    public UomController(UomService uomService) {
        this.uomService = uomService;
    }

    public Mono<ServerResponse> getAllUom(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return uomService.getAllUom(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getUomById(ServerRequest request) {
        int uomId = Integer.parseInt(request.pathVariable("uomId"));

        return uomService.getUomById(uomId)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> createUom(ServerRequest request) {
        Mono<UomMasterEntity> createUomRequestMono = request.bodyToMono(UomMasterEntity.class);

        return createUomRequestMono
                .flatMap(uomService::createUom)
                .flatMap(createdUom -> ServerResponse.status(HttpStatus.CREATED).bodyValue(createdUom))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("Request body is empty..!!"))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateUom(ServerRequest request) {
        int uomId = Integer.parseInt(request.pathVariable("uomId"));
        Mono<UomMasterEntity> updateUomRequestMono = request.bodyToMono(UomMasterEntity.class);

        return updateUomRequestMono.flatMap(updateUomRequest -> uomService.updateUom(uomId, updateUomRequest))
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> deleteUom(ServerRequest request) {
        int uomId = Integer.parseInt(request.pathVariable("uomId"));

        return uomService.deleteUom(uomId)
                .flatMap(deletedUom -> ServerResponse.ok().bodyValue("UOM deleted successfully..!!"))
                .onErrorResume(this::handleError);
    }
}
