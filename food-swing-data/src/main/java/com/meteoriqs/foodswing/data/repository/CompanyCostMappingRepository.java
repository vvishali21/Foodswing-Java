package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.entity.CompanyMealCostMapping;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CompanyCostMappingRepository extends ReactiveCrudRepository<CompanyMealCostMapping, Integer> {
    @Query("SELECT * FROM company_meal_cost_mapping ORDER BY id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<CompanyMealCostMapping> findAllWithPagination (Pageable pageable);

    Mono<CompanyMealCostMapping> findByCompanyIdAndMealIdAndDay(Integer companyId, Integer mealId, Integer dayOfWeek);
}
