package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.MealSuggestionDetails;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MealSuggestionDetailsRepository extends ReactiveCrudRepository<MealSuggestionDetails, Integer>  {

    Flux<MealSuggestionDetails> findByMealSuggestionId(int mealSuggestionId);

    Flux<MealSuggestionDetails> findByMealSuggestionIdAndItemId(int mealSuggestionId, int itemId);
}
