package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.entity.ProcessMaster;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProcessMasterRepository extends ReactiveCrudRepository<ProcessMaster,Integer> {
    Flux<ProcessMaster> findByProcessNameContainsIgnoreCase(String partialName);

    @Query("SELECT * FROM process_master ORDER BY process_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<ProcessMaster> findAllWithPagination (Pageable pageable);

    Mono<ProcessMaster> findByProcessName(String processName);

}
