package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.Grammage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
public interface GrammageRepository extends ReactiveCrudRepository<Grammage, Integer> {

    @Query("SELECT * FROM grammage ORDER BY grammage_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<Grammage> findAllWithPagination (Pageable pageable);

    @Query("SELECT gram FROM grammage WHERE item_id = :itemId LIMIT 1")
    Mono<BigDecimal> findGramByItemId(int itemId);

    Mono<Grammage> findByCompanyIdAndMealIdAndItemIdAndDay(int companyId, int mealId, int itemId, int day);

    Flux<Grammage> findByCompanyIdAndMealIdAndDay(int companyId, int mealId, int dayOfWeek);
}






