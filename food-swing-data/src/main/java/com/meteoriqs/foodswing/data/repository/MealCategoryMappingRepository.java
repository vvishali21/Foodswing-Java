package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.CategoryWithCount;
import com.meteoriqs.foodswing.data.model.MealCategoryMapping;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MealCategoryMappingRepository extends ReactiveCrudRepository<MealCategoryMapping, Integer> {

    Mono<Void> deleteByMealId(int mealId);


    @Query("SELECT category_id, count(*) as count FROM meal_category_mapping WHERE meal_id = :mealId GROUP BY category_id")
    Flux<CategoryWithCount> findCategoryIdsAndCountByMealId(int mealId);


    Flux<MealCategoryMapping> findByMealId(int mealId);

    @Query("SELECT SUM(count) FROM meal_category_mapping WHERE meal_id = :mealId")
    Mono<Integer> getTotalCountByMealId(int mealId);
}
