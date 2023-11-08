package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.entity.ProcessMaster;
import com.meteoriqs.foodswing.production.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
@Component
public class ProcessController extends BaseController{

    private final ProcessService processService;

    @Autowired
    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    public Mono<ServerResponse> getAllProcess(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return processService.getAllProcess(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getProcessById(ServerRequest request) {
        int processId = Integer.parseInt(request.pathVariable("processId"));

        return processService.getProcessById(processId)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> createProcess(ServerRequest request) {
        Mono<ProcessMaster> createProcessRequestMono = request.bodyToMono(ProcessMaster.class);

        return createProcessRequestMono
                .flatMap(processService::createProcess)
                .flatMap(createdProcess -> ServerResponse.status(HttpStatus.CREATED).bodyValue(createdProcess))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("Request body is empty..!!"))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateProcess(ServerRequest request) {
        int processId = Integer.parseInt(request.pathVariable("processId"));
        Mono<ProcessMaster> updateProcessRequestMono = request.bodyToMono(ProcessMaster.class);

        return updateProcessRequestMono.flatMap(updateProcessRequest -> processService.updateProcess
                        (processId, updateProcessRequest))
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> deleteProcess(ServerRequest request) {
        int processId = Integer.parseInt(request.pathVariable("processId"));

        return processService.deleteProcess(processId)
                .flatMap(deletedProcess -> ServerResponse.ok().bodyValue("Process deleted successfully..!!"))
                .onErrorResume(this::handleError);
    }
}
