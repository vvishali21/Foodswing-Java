package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.VesselItemMapping;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface VesselItemMappingRepository extends ReactiveCrudRepository<VesselItemMapping, Integer> {


    @Query("SELECT * FROM vessel_item_mapping ORDER BY id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<VesselItemMapping> findAllWithPagination(Pageable pageable);

    Mono<Void> deleteByVesselId(int vesselId);

    @Query("SELECT item_id FROM vessel_item_mapping WHERE vessel_id = :vesselId")
    Flux<Integer> findItemIdsByVesselId(int vesselId);


    Flux<VesselItemMapping> findByItemId(int itemId);


}
