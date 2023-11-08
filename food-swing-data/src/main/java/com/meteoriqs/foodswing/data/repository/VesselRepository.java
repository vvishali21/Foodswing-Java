package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.Vessel;
import com.meteoriqs.foodswing.data.model.VesselWithItemDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface VesselRepository extends ReactiveCrudRepository<Vessel, Integer> {

    @Query("SELECT * FROM vessel ORDER BY vessel_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<Vessel> findAllWithPagination(Pageable pageable);

    Mono<Vessel> findByVesselName(String vesselName);

    @Query("SELECT v.vessel_id, v.vessel_name,v.status, v.capacity, GROUP_CONCAT(DISTINCT i.item_name ORDER BY i.item_name ASC) AS item_names" +
            " FROM Vessel v" +
            " INNER JOIN vessel_item_mapping m ON v.vessel_id = m.vessel_id" +
            " INNER JOIN item i ON m.item_id = i.item_id GROUP BY v.vessel_id")
    Flux<VesselWithItemDTO> findAllVesselsWithItems();

    Flux<Vessel> findByVesselIdAndStatus(int vesselId, int status);


    @Modifying
    @Query("UPDATE vessel SET status = 1 WHERE vessel_id IN (:vesselIds)")
    Flux<Void> updateVesselStatus(@Param("vesselIds") List<Integer> vesselIds);

    Flux<Vessel> findByVesselIdIn(List<Integer> vesselIds);


}
