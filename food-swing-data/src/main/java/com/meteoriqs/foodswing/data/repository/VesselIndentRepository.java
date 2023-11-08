package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.VesselIndentResponse;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface VesselIndentRepository extends ReactiveCrudRepository<VesselIndentResponse, Integer> {

    @Query("SELECT it.item_name, rd.stage, pm.process_name, mm.medium_name, i.ingredient_id, " +
            "i.ingredient_name, SUM(rd.quantity) AS quantity, uom.name AS name, rd.base_pv, " +
            "rd.ingredient_cost, rd.stage_end_alert_duration, rd.process_id, rd.medium_id, " +
            "rd.base_sv, rd.production_sv, rd.product_pv, rd.duration, rd.duration_unit, " +
            "rd.power, rd.fq, rd.fwd_time, rd.rev_time, rd.start_time, rd.end_time, " +
            "rd.time_taken, rd.stage_duration " +
            "FROM recipe_details rd " +
            "INNER JOIN recipe_master rm ON rm.recipe_id = rd.recipe_id " +
            "INNER JOIN item it ON it.item_id = rm.item_id " +
            "INNER JOIN meal_suggestion_details msd ON rd.recipe_id = msd.recipe_id " +
            "INNER JOIN order_master om ON msd.meal_suggestion_id = om.meal_suggestion_id " +
            "INNER JOIN ingredients i ON i.ingredient_id = rd.ingredient_id " +
            "INNER JOIN uom_master uom ON uom.uom_id = i.uom_id " +
            "INNER JOIN process_master pm ON pm.process_id = rd.process_id " +
            "INNER JOIN medium_master mm ON mm.medium_id = rd.medium_id " +
            "WHERE om.order_date = :orderDate AND om.meal_id = :mealId " +
            "GROUP BY rm.item_id, rd.stage, rd.recipe_detail_id, rd.process_id, rd.medium_id, rd.ingredient_id " +
            "ORDER BY rm.item_id, rd.stage, rd.recipe_detail_id")
    Flux<VesselIndentResponse> getVesselIndent(@Param("orderDate") String orderDate, @Param("mealId") int mealId);

    @Query("SELECT it.item_name, rd.stage, pm.process_name, mm.medium_name, i.ingredient_id, " +
            "i.ingredient_name, SUM(rd.quantity) AS quantity, uom.name AS name, rd.base_pv, " +
            "rd.ingredient_cost, rd.stage_end_alert_duration, rd.process_id, rd.medium_id, " +
            "rd.base_sv, rd.production_sv, rd.product_pv, rd.duration, rd.duration_unit, " +
            "rd.power, rd.fq, rd.fwd_time, rd.rev_time, rd.start_time, rd.end_time, " +
            "rd.time_taken, rd.stage_duration " +
            "FROM recipe_details rd " +
            "INNER JOIN recipe_master rm ON rm.recipe_id = rd.recipe_id " +
            "INNER JOIN item it ON it.item_id = rm.item_id " +
            "INNER JOIN meal_suggestion_details msd ON rd.recipe_id = msd.recipe_id " +
            "INNER JOIN order_master om ON msd.meal_suggestion_id = om.meal_suggestion_id " +
            "INNER JOIN ingredients i ON i.ingredient_id = rd.ingredient_id " +
            "INNER JOIN uom_master uom ON uom.uom_id = i.uom_id " +
            "INNER JOIN process_master pm ON pm.process_id = rd.process_id " +
            "INNER JOIN medium_master mm ON mm.medium_id = rd.medium_id " +
            "WHERE om.indent_sent = 'N' " +
            "GROUP BY rm.item_id, rd.stage, rd.recipe_detail_id, rd.process_id, rd.medium_id, rd.ingredient_id " +
            "ORDER BY rm.item_id, rd.stage, rd.recipe_detail_id")
    Flux<VesselIndentResponse> getUnSentVesselIndent();


    @Modifying
    @Query("UPDATE order_master SET indent_sent = 'Y' WHERE indent_sent = 'N'")
    Mono<Integer> markSentOrders();


}
