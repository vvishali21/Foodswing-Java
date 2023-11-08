package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.entity.MealProductionPlan;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MealProductionPlanRepository extends ReactiveCrudRepository<MealProductionPlan, Integer> {

    Flux<MealProductionPlan> findByPlanId(int planId);

}
