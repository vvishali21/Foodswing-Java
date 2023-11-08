package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.model.PaginationInfo;
import com.meteoriqs.foodswing.data.model.RecipeSearchResponse;
import com.meteoriqs.foodswing.data.model.Status;
import com.meteoriqs.foodswing.data.model.UomMasterEntity;
import com.meteoriqs.foodswing.data.repository.UomMasterRepository;
import net.spy.memcached.MemcachedClient;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class UomService extends BaseService {

    private final UomMasterRepository uomMasterRepository;

    private final MemcachedClient memcachedClient;

    public UomService(UomMasterRepository uomMasterRepository, MemcachedClient memcachedClient) {
        this.uomMasterRepository = uomMasterRepository;
        this.memcachedClient = memcachedClient;
    }

    public Mono<CustomPaginateResponse<List<UomMasterEntity>>> getAllUom(Pageable pageable) {

        return uomMasterRepository.count()
                .flatMap(totalCount -> uomMasterRepository.findAllWithPagination(pageable)
                        .collectList()
                        .map(paginatedUom -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<UomMasterEntity>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedUom);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All UOM Error Occurred")));
    }

    public Mono<CustomResponse<UomMasterEntity>> getUomById(int uomId) {
        return uomMasterRepository.findById(uomId)
                .map(uom -> new CustomResponse<>(new Status(200, "Success"), uom))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }

    public Mono<CustomResponse<Object>> createUom(UomMasterEntity createUomRequest) {
        return uomMasterRepository.findByName(createUomRequest.getName())
                .flatMap(existingUom -> Mono.just(new CustomResponse<>(new Status(403, "UOM name already exists"), null)))
                .switchIfEmpty(Mono.defer(() -> {
                    UomMasterEntity newUom = new UomMasterEntity();
                    newUom.setName(createUomRequest.getName());
                    newUom.setActive(true);
                    newUom.setCreatedBy(createUomRequest.getCreatedBy());
                    newUom.setCreatedTime(Instant.now());
                    return uomMasterRepository.save(newUom)
                            .flatMap(savedUom -> {
                                memcachedClient.set("uomNames-"+savedUom.getUomId(),0,savedUom.getName());

                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("uomEntityList");
                                processData.add(new RecipeSearchResponse(savedUom.getUomId(),savedUom.getName(),0,null,null));
                                memcachedClient.set("uomEntityList",0,processData);

                                return Mono.just(new CustomResponse<>(new Status(201, "UOM created successfully"), savedUom));
                            });
                }));
    }

    public Mono<CustomResponse<UomMasterEntity>> updateUom(int uomId, UomMasterEntity updateUomRequest) {
        return uomMasterRepository.findById(uomId)
                .flatMap(existingUom -> {
                    existingUom.setName(updateUomRequest.getName());
                    existingUom.setUpdatedBy(updateUomRequest.getUpdatedBy());
                    existingUom.setUpdatedTime(Instant.now());

                    return uomMasterRepository.save(existingUom)
                            .doOnNext(savedUom -> {
                                memcachedClient.set("uomNames-" + savedUom.getUomId(), 0, savedUom.getName());
                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("uomEntityList");
                                processData.add(new RecipeSearchResponse(savedUom.getUomId(),savedUom.getName(),0,null,null));
                                memcachedClient.set("uomEntityList",0,processData);
                            });
                })
                .map(uom -> new CustomResponse<>(new Status(200, "UOM updated successfully"), uom))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }


    public Mono<UomMasterEntity> deleteUom(int uomId) {
        return uomMasterRepository.findById(uomId)
                .flatMap(uom -> uomMasterRepository.deleteById(uomId).thenReturn(uom))
                .doOnNext(deletedUom -> memcachedClient.delete("uomNames-"+uomId))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "UOM not found..!!"))));
    }

    private CustomResponse<UomMasterEntity> getNotFoundResponse() {
        return new CustomResponse<>(new Status(404, "UOM not found"), null);
    }
}
