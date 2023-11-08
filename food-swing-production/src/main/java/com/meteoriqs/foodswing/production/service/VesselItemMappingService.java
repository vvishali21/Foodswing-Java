package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.model.PaginationInfo;
import com.meteoriqs.foodswing.data.model.Status;
import com.meteoriqs.foodswing.data.model.VesselItemMapping;
import com.meteoriqs.foodswing.data.repository.VesselItemMappingRepository;
import net.spy.memcached.MemcachedClient;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class VesselItemMappingService extends BaseService {

    private final VesselItemMappingRepository vesselItemMappingRepository;

    private final MemcachedClient memcachedClient;

    public VesselItemMappingService(VesselItemMappingRepository vesselItemMappingRepository,
                                    MemcachedClient memcachedClient) {
        this.vesselItemMappingRepository = vesselItemMappingRepository;
        this.memcachedClient = memcachedClient;
    }


    public Mono<CustomPaginateResponse<List<VesselItemMapping>>> getAllVesselItem(Pageable pageable) {
        return vesselItemMappingRepository.count()
                .flatMap(totalCount -> vesselItemMappingRepository.findAllWithPagination(pageable)
                        .flatMap(vesselItemMapping -> {
                            // Fetch item name using item id from memcached
                            String itemName = (String) memcachedClient.get("itemNames-" + vesselItemMapping.getItemId());
                            // Fetch vessel name using vessel id from memcached
                            String vesselName = (String) memcachedClient.get("vesselNames-" + vesselItemMapping.getVesselId());

                            // Set itemName and vesselName to the vesselItemMapping object
                            vesselItemMapping.setItemName(itemName);
                            vesselItemMapping.setVesselName(vesselName);

                            return Mono.just(vesselItemMapping);
                        })
                        .collectList()
                        .map(paginatedVesselItem -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<VesselItemMapping>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedVesselItem);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All VesselItem Error Occurred")));
    }




    public Mono<CustomResponse<List<VesselItemMapping>>> updateVesselItem(Flux<VesselItemMapping> updateVesselRequests) {
        return updateVesselRequests
                .flatMap(updateVesselRequest -> vesselItemMappingRepository.findById(updateVesselRequest.getId())
                        .flatMap(existingVesselItem -> {
                            if (existingVesselItem != null) {
                                existingVesselItem.setVesselId(updateVesselRequest.getVesselId());
                                existingVesselItem.setMaxCapacity(updateVesselRequest.getMaxCapacity());
                                existingVesselItem.setItemId(updateVesselRequest.getItemId());
                                existingVesselItem.setUpdatedBy(updateVesselRequest.getUpdatedBy());
                                existingVesselItem.setUpdatedTime(Instant.now());

                                return vesselItemMappingRepository.save(existingVesselItem);
                            } else {
                                return Mono.empty();
                            }
                        })
                )
                .collectList()
                .flatMap(updatedVessel -> {
                    if (!updatedVessel.isEmpty()) {
                        return Mono.just(new CustomResponse<>(new Status(200, "VesselItem updated successfully"), updatedVessel));
                    } else {
                        return Mono.just(new CustomResponse<>(new Status(404, "VesselItem not found"), null));
                    }
                });
    }


}
