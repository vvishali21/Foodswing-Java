package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.UomMasterEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UomMasterRepository extends ReactiveCrudRepository<UomMasterEntity,Integer> {

    @Query("SELECT * FROM uom_master ORDER BY uom_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<UomMasterEntity> findAllWithPagination (Pageable pageable);

    Mono<UomMasterEntity> findByName(String name);

}
