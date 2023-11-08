package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.Item;
import com.meteoriqs.foodswing.data.model.ItemWithDetailsDTO;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

@Repository
public interface ItemRepository extends ReactiveCrudRepository<Item, Integer> {

    @Query("SELECT * FROM item ORDER BY item_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<Item> findAllWithPagination (Pageable pageable);

    Flux<Item> findByItemNameContainsIgnoreCase(String partialName);

    Mono<Item> findByItemName(String itemName);


    @Query("SELECT i.item_id, i.item_name, i.uom_id, uom.uom_id, uom.name AS uom_name, i.gram, i.weight, c.category_name, " +
            "GROUP_CONCAT(DISTINCT m.name ORDER BY m.name ASC) AS meal_name " +
            "FROM item i " +
            "INNER JOIN meal_item_mapping mimap ON i.item_id = mimap.item_id " +
            "INNER JOIN category c ON c.category_id = mimap.category_id " +
            "INNER JOIN grammage g ON g.item_id = i.item_id " +
            "INNER JOIN uom_master uom ON uom.uom_id = i.uom_id " +
            "INNER JOIN meal m ON m.meal_id = mimap.meal_id " +
            "GROUP BY i.item_id, c.category_name")
    Flux<ItemWithDetailsDTO> findAllItemsWithDetails();


    Flux<Item> findByItemId(int itemId);
}

