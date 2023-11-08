package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.entity.ProcessMaster;
import com.meteoriqs.foodswing.data.model.PaginationInfo;
import com.meteoriqs.foodswing.data.model.RecipeSearchResponse;
import com.meteoriqs.foodswing.data.model.Status;
import com.meteoriqs.foodswing.data.repository.ProcessMasterRepository;
import net.spy.memcached.MemcachedClient;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class ProcessService extends BaseService {

    private final ProcessMasterRepository processMasterRepository;

    private final MemcachedClient memcachedClient;

    public ProcessService(ProcessMasterRepository processMasterRepository, MemcachedClient memcachedClient) {
        this.processMasterRepository = processMasterRepository;
        this.memcachedClient = memcachedClient;
    }

    public Mono<CustomPaginateResponse<List<ProcessMaster>>> getAllProcess(Pageable pageable) {

        return processMasterRepository.count()
                .flatMap(totalCount -> processMasterRepository.findAllWithPagination(pageable)
                        .collectList()
                        .map(paginatedProcess -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<ProcessMaster>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedProcess);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All Process Error Occurred")));
    }

    public Mono<CustomResponse<ProcessMaster>> getProcessById(int processId) {
        return processMasterRepository.findById(processId)
                .map(process -> new CustomResponse<>(new Status(200, "Success"), process))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }


    public Mono<CustomResponse<Object>> createProcess(ProcessMaster createProcessRequest) {
        return processMasterRepository.findByProcessName(createProcessRequest.getProcessName())
                .flatMap(existingProcess -> Mono.just(new CustomResponse<>(new Status(403, "Process name already exists"), null)))
                .switchIfEmpty(Mono.defer(() -> {
                    ProcessMaster newProcess = new ProcessMaster();
                    newProcess.setProcessName(createProcessRequest.getProcessName());
                    newProcess.setActive(true);
                    newProcess.setCreatedBy(createProcessRequest.getCreatedBy());
                    newProcess.setCreatedTime(Instant.now());
                    return processMasterRepository.save(newProcess)
                            .flatMap(savedProcess -> {
                                memcachedClient.set("processNames-"+savedProcess.getProcessId(),0, savedProcess.getProcessName());

                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("processEntityList");
                                processData.add(new RecipeSearchResponse(savedProcess.getProcessId(),savedProcess.getProcessName(),0,null,null));
                                memcachedClient.set("processEntityList",0,processData);

                                return Mono.just(new CustomResponse<>(new Status(201, "Process created successfully"), savedProcess));
                            });
                }));
    }


    public Mono<CustomResponse<ProcessMaster>> updateProcess(int processId, ProcessMaster updateProcessRequest) {
        return processMasterRepository.findById(processId)
                .flatMap(existingProcess -> {
                    existingProcess.setProcessName(updateProcessRequest.getProcessName());
                    existingProcess.setUpdatedBy(updateProcessRequest.getUpdatedBy());
                    existingProcess.setUpdatedTime(Instant.now());

                    return processMasterRepository.save(existingProcess)
                            .doOnNext(savedProcess -> {
                                memcachedClient.set("processNames-" + savedProcess.getProcessId(), 0, savedProcess.getProcessName());
                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("processEntityList");
                                processData.add(new RecipeSearchResponse(savedProcess.getProcessId(),savedProcess.getProcessName(),0,null,null));
                                memcachedClient.set("processEntityList",0,processData);
                            });
                })
                .map(process -> new CustomResponse<>(new Status(200, "Process updated successfully"), process))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }

    public Mono<ProcessMaster> deleteProcess(int processId) {
        return processMasterRepository.findById(processId)
                .flatMap(process -> processMasterRepository.deleteById(processId).thenReturn(process))
                .doOnNext(deletedProcess -> memcachedClient.delete("processNames-"+processId))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Process not found..!!"))));
    }

    private CustomResponse<ProcessMaster> getNotFoundResponse() {
        return new CustomResponse<>(new Status(404, "Process not found"), null);
    }
}
