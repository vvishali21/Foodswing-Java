package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.HopperIndentResponse;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
@Repository
public interface HopperIndentRepository extends ReactiveCrudRepository<HopperIndentResponse, Integer> {

    @Query("SELECT it.item_name, i.ingredient_id, i.ingredient_name, SUM(rd.quantity) AS quantity, uom.name " +
            "FROM recipe_details rd " +
            "INNER JOIN recipe_master rm ON rm.recipe_id = rd.recipe_id " +
            "INNER JOIN item it ON it.item_id = rm.item_id " +
            "INNER JOIN meal_suggestion_details msd ON rd.recipe_id = msd.recipe_id " +
            "INNER JOIN order_master om ON msd.meal_suggestion_id = om.meal_suggestion_id " +
            "INNER JOIN ingredients i ON i.ingredient_id = rd.ingredient_id " +
            "INNER JOIN uom_master uom ON uom.uom_id = i.uom_id " +
            "WHERE om.order_date = :orderDate AND om.meal_id = :mealId " +
            "GROUP BY rd.ingredient_id, rm.item_id")
    Flux<HopperIndentResponse> getHopperIndent(@Param("orderDate") String orderDate, @Param("mealId") int mealId);
}

