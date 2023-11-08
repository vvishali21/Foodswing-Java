package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.model.Grammage;
import com.meteoriqs.foodswing.data.model.PaginationInfo;
import com.meteoriqs.foodswing.data.model.Status;
import com.meteoriqs.foodswing.data.repository.GrammageRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class GrammageService extends BaseService {

    private final GrammageRepository grammageRepository;

    public GrammageService(GrammageRepository grammageRepository) {
        this.grammageRepository = grammageRepository;
    }

    public Mono<CustomPaginateResponse<List<Grammage>>> getAllGram(Pageable pageable) {

        return grammageRepository.count()
                .flatMap(totalCount -> grammageRepository.findAllWithPagination(pageable)
                        .collectList()
                        .map(paginatedGram -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<Grammage>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedGram);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All Grammage Error Occurred")));
    }


    public Mono<CustomResponse<List<Grammage>>> updateGrammage(Flux<Grammage> updateGrammageRequests) {
        return updateGrammageRequests
                .flatMap(updateGrammageRequest -> grammageRepository.findById(updateGrammageRequest.getGrammageId())
                        .flatMap(existingGrammage -> {
                            if (existingGrammage != null) {
                                existingGrammage.setCompanyId(updateGrammageRequest.getCompanyId());
                                existingGrammage.setMealId(updateGrammageRequest.getMealId());
                                existingGrammage.setGram(updateGrammageRequest.getGram());
                                existingGrammage.setDay(updateGrammageRequest.getDay());
                                existingGrammage.setItemId(updateGrammageRequest.getItemId());
                                existingGrammage.setCreatedBy(updateGrammageRequest.getCreatedBy());
                                existingGrammage.setCreatedTime(updateGrammageRequest.getCreatedTime());
                                existingGrammage.setUpdatedBy(updateGrammageRequest.getUpdatedBy());
                                existingGrammage.setUpdatedTime(Instant.now());

                                return grammageRepository.save(existingGrammage);
                            } else {
                                return Mono.empty();
                            }
                        })
                )
                .collectList()
                .flatMap(updatedGrammages -> {
                    if (!updatedGrammages.isEmpty()) {
                        return Mono.just(new CustomResponse<>(new Status(200, "Grammage updated successfully"), updatedGrammages));
                    } else {
                        return Mono.just(new CustomResponse<>(new Status(404, "Grammage not found"), null));
                    }
                });
    }
}
