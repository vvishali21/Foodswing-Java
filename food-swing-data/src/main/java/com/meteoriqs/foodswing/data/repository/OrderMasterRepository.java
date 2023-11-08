package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.OrderMaster;
import com.meteoriqs.foodswing.data.model.PlanList;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderMasterRepository extends ReactiveCrudRepository<OrderMaster, Integer> {

    Flux<OrderMaster> findByMealIdAndOrderDateAndCompanyIdIn(int mealId, LocalDate orderDate, List<Integer> companyIdList);

    Flux<OrderMaster> findByMealIdAndOrderDate(int mealId, String orderDate);

    Flux<OrderMaster> findByMealIdAndOrderDateAndMealSuggestionId(int mealId, String orderDate, int mealSuggestionId);

    @Query("SELECT om.meal_suggestion_id, ms.plan_id, " +
            "GROUP_CONCAT(om.company_id) as company_ids, " +
            "GROUP_CONCAT(c.company_name) as company_names, " +
            "SUM(om.meal_count) as meal_count, " +
            "om.meal_id, m.name, om.day_of_week, om.status, om.order_date " +
            "FROM order_master om " +
            "JOIN company c ON FIND_IN_SET(c.company_id, om.company_id) > 0 " +
            "JOIN meal m ON om.meal_id = m.meal_id " +
            "JOIN meal_suggestion ms ON om.meal_suggestion_id = ms.meal_suggestion_id " +
            "WHERE om.meal_id = :mealId AND om.order_date = :orderDate AND om.meal_suggestion_id > 0 " +
            "GROUP BY om.meal_suggestion_id, ms.plan_id, om.meal_id, m.name, om.day_of_week, om.status, om.order_date")
    Flux<PlanList> findByOrderDateAndMealId(int mealId, String orderDate);


    @Query("select sum(meal_count) from order_master where meal_suggestion_id = :mealSuggestionId")
    Mono<Integer> sumMealCountByMealSuggestionId(Integer mealSuggestionId);

    @Query("SELECT * FROM order_master ORDER BY order_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<OrderMaster> findAllWithPagination(Pageable pageable);

    Mono<OrderMaster> findByOrderDateAndMealIdAndCompanyId(LocalDate orderDate, int mealId, int companyId);

    Flux<OrderMaster> findByOrderDateAndMealId(String orderDate, int mealId);

    Flux<OrderMaster> findByCompanyIdAndMealIdAndOrderDate(Integer companyId, Integer mealId, String orderDate);

    @Modifying
    @Query("UPDATE order_master SET meal_suggestion_id = 0 WHERE meal_suggestion_id = :mealSuggestionId")
    Mono<Integer> updateMealSuggestionId(int mealSuggestionId);

    @Modifying
    @Query("UPDATE order_master SET status = 2 WHERE meal_suggestion_id = :mealSuggestionId")
    Mono<Void> updateOrderStatus(int mealSuggestionId);

    @Modifying
    @Query("UPDATE order_master SET status = :status WHERE meal_suggestion_id = :mealSuggestionId")
    Mono<Void> updateStatusByMealSuggestionId(int status, int mealSuggestionId);


}

