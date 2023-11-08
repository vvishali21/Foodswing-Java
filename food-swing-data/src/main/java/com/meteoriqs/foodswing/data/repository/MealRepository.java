package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.Meal;
import com.meteoriqs.foodswing.data.model.MealWithCategoriesDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MealRepository extends ReactiveCrudRepository<Meal, Integer> {


    @Query("SELECT * FROM meal ORDER BY meal_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<Meal> findAllWithPagination (Pageable pageable);

    Mono<Meal> findByName(String name);

    @Query("SELECT m.meal_id, m.name, GROUP_CONCAT(DISTINCT c.category_name ORDER BY c.category_name ASC) AS " +
            "category_names FROM meal m INNER JOIN meal_category_mapping mc ON m.meal_id = mc.meal_id " +
            "INNER JOIN category c ON mc.category_id = c.category_id GROUP BY m.meal_id")
    Flux<MealWithCategoriesDTO> findAllMealsWithCategories();



}
