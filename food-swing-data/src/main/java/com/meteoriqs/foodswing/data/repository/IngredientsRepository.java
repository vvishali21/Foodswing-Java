package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.Ingredients;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;

@Repository
public interface IngredientsRepository extends ReactiveCrudRepository<Ingredients, Integer> {

    @Query("SELECT * FROM ingredients ORDER BY ingredient_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<Ingredients> findAllWithPagination(Pageable pageable);

    Flux<Ingredients> findByIngredientNameContainsIgnoreCase(String partialName);


    Mono<Ingredients> findByIngredientName(String ingredientName);

}

