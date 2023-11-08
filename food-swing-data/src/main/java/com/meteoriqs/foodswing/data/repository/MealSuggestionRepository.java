package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.MealSuggestion;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface MealSuggestionRepository extends ReactiveCrudRepository<MealSuggestion, Integer>  {

    @Query("SELECT plan_id FROM meal_suggestion WHERE meal_suggestion_id = :mealSuggestionId")
    Mono<Integer> getPlanIdByMealSuggestionId(Integer mealSuggestionId);

}
