package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.entity.RecipeMaster;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface RecipeMasterRepository extends ReactiveCrudRepository<RecipeMaster,Integer> {
    @Query("SELECT * FROM recipe_master ORDER BY recipe_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<RecipeMaster> findAllWithPagination(Pageable pageable);

    @Query("SELECT rm.* FROM recipe_master rm INNER JOIN item i ON rm.item_id = i.item_id WHERE i.item_name LIKE CONCAT" +
            "('%', :partialName, '%') ORDER BY rm.recipe_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<RecipeMaster> findByPartialItemNameWithPagination(@Param("partialName") String partialName, Pageable pageable);

    @Query("SELECT COUNT(*) FROM recipe_master rm INNER JOIN item i ON rm.item_id = i.item_id WHERE i.item_name LIKE CONCAT" +
            "('%', :partialName, '%')")
    Mono<Integer> countByPartialItemName(@Param("partialName") String partialName);

    Flux<RecipeMaster> findByItemId(int itemId);

    Flux<RecipeMaster> findByRecipeDescriptionContainsIgnoreCase(String partialName);

    Flux<RecipeMaster> findByItemIdIn(List<Integer> itemIdList);

    @Query("SELECT DISTINCT i.item_name FROM meal_item_mapping m " +
            "LEFT JOIN recipe_master r ON m.item_id = r.item_id " +
            "JOIN item i ON m.item_id = i.item_id " +
            "WHERE r.item_id IS NULL")
    Flux<String> findItemNamesWithoutRecipe();

}
