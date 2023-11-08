package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CategoryRepository extends ReactiveCrudRepository<Category, Integer> {

    @Query("SELECT * FROM category ORDER BY category_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<Category> findAllWithPagination (Pageable pageable);

    Mono<Category> findByCategoryName(String categoryName);

}
