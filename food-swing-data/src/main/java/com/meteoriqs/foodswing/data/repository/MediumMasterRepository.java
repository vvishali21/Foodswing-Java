package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.entity.MediumMaster;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MediumMasterRepository extends ReactiveCrudRepository<MediumMaster,Integer> {
    Flux<MediumMaster> findByMediumNameContainsIgnoreCase(String partialName);

    @Query("SELECT * FROM medium_master ORDER BY medium_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<MediumMaster> findAllWithPagination (Pageable pageable);

    Mono<MediumMaster> findByMediumName(String mediumName);

}
