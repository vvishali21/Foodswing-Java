package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.MealItemMapping;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface MealItemMappingRepository extends ReactiveCrudRepository<MealItemMapping, Integer> {

    @Query("SELECT * FROM meal_item_mapping ORDER BY id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<MealItemMapping> findAllWithPagination (Pageable pageable);

    Flux<MealItemMapping> findByMealIdAndCategoryIdIn(int mealId, List<Integer> categoryIds);

    Mono<Void> deleteByItemId(int itemId);

    @Query("SELECT meal_id FROM meal_item_mapping WHERE item_id = :itemId")
    Flux<Integer> findMealIdsByItemId(int itemId);

    @Query("SELECT category_id FROM meal_item_mapping WHERE item_id = :itemId")
    Flux<Integer> findCategoryIdsByItemId(int itemId);


}
