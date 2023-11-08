package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.entity.RecipeDetailsEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RecipeDetailsRepository extends ReactiveCrudRepository<RecipeDetailsEntity, Integer> {
    Flux<RecipeDetailsEntity> findByRecipeId(int recipeId);

    Mono<Void> deleteByRecipeId(int recipeId);
}
