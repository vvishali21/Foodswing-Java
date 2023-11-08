package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.data.repository.VesselItemMappingRepository;
import com.meteoriqs.foodswing.data.repository.VesselRepository;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VesselService extends BaseService {
    private static final Logger logger = LoggerFactory.getLogger(VesselService.class);
    private final VesselRepository vesselRepository;
    private final VesselItemMappingRepository vesselItemMappingRepository;
    private final MemcachedClient memcachedClient;

    public VesselService(VesselRepository vesselRepository, VesselItemMappingRepository vesselItemMappingRepository,
                         MemcachedClient memcachedClient) {
        this.vesselRepository = vesselRepository;
        this.vesselItemMappingRepository = vesselItemMappingRepository;
        this.memcachedClient = memcachedClient;
    }

    public Mono<CustomPaginateResponse<List<VesselWithItemDTO>>> getAllVessel(Pageable pageable) {

        return vesselRepository.count()
                .flatMap(totalCount -> vesselRepository.findAllVesselsWithItems()
                        .collectList()
                        .map(paginatedVessel -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<VesselWithItemDTO>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedVessel);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All Vessel Error Occurred")));
    }


    public Mono<CustomResponse<VesselItemMappingResponse>> getVesselById(int vesselId) {

        Mono<Vessel> vesselMono = vesselRepository.findById(vesselId);

        Flux<Integer> itemIdsFlux = vesselItemMappingRepository.findItemIdsByVesselId(vesselId);
        Mono<List<Integer>> itemIdsMono = itemIdsFlux.collectList();

        return Mono.zip(vesselMono, itemIdsMono)
                .map(tuple -> {
                    Vessel vessel = tuple.getT1();
                    List<Integer> itemIds = tuple.getT2();

                    VesselItemMappingResponse response = new VesselItemMappingResponse();
                    response.setVesselId(vessel.getVesselId());
                    response.setVesselName(vessel.getVesselName());
                    response.setCapacity(vessel.getCapacity());
                    response.setVesselType(vessel.getVesselType());
                    response.setUomId(vessel.getUomId());
                    response.setStatus(vessel.getStatus());
                    response.setItemIds(itemIds);
                    response.setCreatedBy(vessel.getCreatedBy());
                    response.setCreatedTime(vessel.getCreatedTime());
                    response.setUpdatedBy(vessel.getUpdatedBy());
                    response.setUpdatedTime(vessel.getUpdatedTime());

                    return new CustomResponse<>(new Status(200, "Success"), response);
                })
                .switchIfEmpty(Mono.just(getNotFoundVesselResponse()));
    }

    private CustomResponse<VesselItemMappingResponse> getNotFoundVesselResponse() {
        return new CustomResponse<>(new Status(404, "Vessel not found"), null);
    }


    public Mono<CustomResponse<Object>> createVessel(VesselItemMappingResponse createVesselRequest) {
        return vesselRepository.findByVesselName(createVesselRequest.getVesselName())
                .flatMap(existingVessel -> Mono.just(new CustomResponse<>(new Status(403, "Vessel name already exists"), null)))
                .switchIfEmpty(Mono.defer(() -> {
                    Vessel newVessel = new Vessel();
                    newVessel.setVesselName(createVesselRequest.getVesselName());
                    newVessel.setStatus(createVesselRequest.getStatus());
                    newVessel.setCapacity(createVesselRequest.getCapacity());
                    newVessel.setVesselType(createVesselRequest.getVesselType());
                    newVessel.setUomId(createVesselRequest.getUomId());
                    newVessel.setActive(true);
                    newVessel.setCreatedBy(createVesselRequest.getCreatedBy());
                    newVessel.setCreatedTime(Instant.now());
                    return vesselRepository.save(newVessel)
                            .flatMap(savedVessel -> {
                                List<Integer> itemIds = createVesselRequest.getItemIds();
                                List<VesselItemMapping> mappings = itemIds.stream()
                                        .map(itemId -> {
                                            VesselItemMapping mapping = new VesselItemMapping();
                                            mapping.setVesselId(savedVessel.getVesselId());
                                            mapping.setItemId(itemId);
                                            mapping.setMaxCapacity(savedVessel.getCapacity());
                                            mapping.setCreatedBy(savedVessel.getCreatedBy());
                                            mapping.setCreatedTime(Instant.now());
                                            return mapping;
                                        })
                                        .collect(Collectors.toList());

                                return vesselItemMappingRepository.saveAll(mappings)
                                        .collectList()
                                        .flatMap(savedMappings -> {
                                            memcachedClient.set("vesselNames-" + savedVessel.getVesselId(), 0, savedVessel.getVesselName());
                                            return Mono.just(new CustomResponse<>(new Status(201, "Vessel created successfully"), savedVessel));
                                        });
                            });
                }))
                .onErrorResume(throwable -> {
                    logger.error("Error creating vessel: {}", throwable.getMessage());
                    return Mono.just(new CustomResponse<>(new Status(500, "Failed to create vessel"), null));
                });
    }


    public Mono<CustomResponse<Vessel>> updateVessel(int vesselId, VesselItemMappingResponse updateVesselRequest) {
        return vesselRepository.findById(vesselId)
                .flatMap(existingVessel -> {
                    existingVessel.setVesselName(updateVesselRequest.getVesselName());
                    existingVessel.setStatus(updateVesselRequest.getStatus());
                    existingVessel.setCapacity(updateVesselRequest.getCapacity());
                    existingVessel.setUomId(updateVesselRequest.getUomId());
                    existingVessel.setVesselType(updateVesselRequest.getVesselType());
                    existingVessel.setUpdatedBy(updateVesselRequest.getUpdatedBy());
                    existingVessel.setUpdatedTime(Instant.now());

                    return vesselRepository.save(existingVessel)
                            .doOnNext(savedVessel ->
                                    memcachedClient.set("vesselNames-" + savedVessel.getVesselId(), 0, savedVessel.getVesselName()))
                            .then(vesselItemMappingRepository.deleteByVesselId(existingVessel.getVesselId())
                                    .thenMany(Flux.fromIterable(updateVesselRequest.getItemIds()))
                                    .flatMap(itemId -> {
                                        VesselItemMapping vesselItemMapping = new VesselItemMapping();
                                        vesselItemMapping.setVesselId(existingVessel.getVesselId());
                                        vesselItemMapping.setItemId(itemId);
                                        vesselItemMapping.setMaxCapacity(updateVesselRequest.getCapacity());
                                        vesselItemMapping.setCreatedBy(updateVesselRequest.getCreatedBy());
                                        vesselItemMapping.setCreatedTime(Instant.now());
                                        vesselItemMapping.setUpdatedBy(updateVesselRequest.getUpdatedBy());
                                        vesselItemMapping.setUpdatedTime(Instant.now());
                                        vesselItemMapping.setActive(true);

                                        return vesselItemMappingRepository.save(vesselItemMapping);
                                    })
                                    .then())
                            .thenReturn(new CustomResponse<>(new Status(200, "Vessel updated successfully"), existingVessel));
                })
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }


    public Mono<Vessel> deleteVessel(int vesselId) {
        return vesselRepository.findById(vesselId)
                .flatMap(vessel -> vesselRepository.deleteById(vesselId).thenReturn(vessel))
                .doOnNext(deletedVessel -> memcachedClient.delete("vesselNames-" + vesselId))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Vessel not found..!!"))));
    }

    private CustomResponse<Vessel> getNotFoundResponse() {
        return new CustomResponse<>(new Status(404, "Vessel not found"), null);
    }
}
