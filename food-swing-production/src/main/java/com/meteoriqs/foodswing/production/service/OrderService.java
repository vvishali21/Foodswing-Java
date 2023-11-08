package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.data.repository.CompanyRepository;
import com.meteoriqs.foodswing.data.repository.MealRepository;
import com.meteoriqs.foodswing.data.repository.OrderMasterRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class OrderService extends BaseService {

    private final OrderMasterRepository orderMasterRepository;
    private final CompanyRepository companyRepository;
    private final MealRepository mealRepository;

    public OrderService(OrderMasterRepository orderMasterRepository, CompanyRepository companyRepository,
                        MealRepository mealRepository) {
        this.orderMasterRepository = orderMasterRepository;
        this.companyRepository = companyRepository;
        this.mealRepository = mealRepository;
    }

    public Mono<CustomPaginateResponse<List<OrderMaster>>> getAllOrder(Pageable pageable) {
        return orderMasterRepository.count()
                .flatMap(totalCount -> orderMasterRepository.findAllWithPagination(pageable)
                        .flatMap(order -> {
                            Mono<String> companyNameMono = companyRepository.findById(order.getCompanyId())
                                    .map(Company::getCompanyName);

                            Mono<String> mealNameMono = mealRepository.findById(order.getMealId())
                                    .map(Meal::getName);

                            return Mono.zip(companyNameMono, mealNameMono)
                                    .map(tuple -> {
                                        order.setCompanyName(tuple.getT1());
                                        order.setMealName(tuple.getT2());
                                        return order;
                                    });
                        })
                        .collectList()
                        .map(paginatedOrder -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<OrderMaster>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedOrder);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All Order Error Occurred")));
    }

    public Mono<CustomResponse<OrderMaster>> getOrderById(int orderId) {
        return orderMasterRepository.findById(orderId)
                .flatMap(order -> {
                    Mono<String> companyNameMono = companyRepository.findById(order.getCompanyId())
                            .map(Company::getCompanyName);

                    Mono<String> mealNameMono = mealRepository.findById(order.getMealId())
                            .map(Meal::getName);

                    return Mono.zip(companyNameMono, mealNameMono)
                            .map(tuple -> {
                                order.setCompanyName(tuple.getT1());
                                order.setMealName(tuple.getT2());
                                return order;
                            });
                })
                .map(orderWithNames -> new CustomResponse<>(new Status(200, "Success"), orderWithNames))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }

    public Mono<CustomResponse<OrderMaster>> createOrder(OrderMaster createMealRequest) {
        // Check if an order with the same order date, meal ID, and company ID exists
        return orderMasterRepository.findByOrderDateAndMealIdAndCompanyId(
                        createMealRequest.getOrderDate(),
                        createMealRequest.getMealId(),
                        createMealRequest.getCompanyId())
                .flatMap(existingOrder -> Mono.just(new CustomResponse<>(
                        new Status(409, "Order already exists"), existingOrder)))
                .switchIfEmpty(
                        Mono.defer(() -> {
                            // No existing order found, proceed to create a new order
                            OrderMaster newOrder = new OrderMaster();
                            newOrder.setCompanyId(createMealRequest.getCompanyId());
                            newOrder.setMealId(createMealRequest.getMealId());
                            newOrder.setMealCount(createMealRequest.getMealCount());
                            newOrder.setOrderDate(createMealRequest.getOrderDate());
                            // newOrder.setMealBudget(createMealRequest.getMealBudget());
                            // newOrder.setMealSuggestionId(createMealRequest.getMealSuggestionId());
                            // newOrder.setMealStartTime(createMealRequest.getMealStartTime());
                            newOrder.setDayOfWeek(createMealRequest.getDayOfWeek());
                            newOrder.setActive(true);
                            newOrder.setCreatedBy(createMealRequest.getCreatedBy());
                            newOrder.setCreatedTime(Instant.now());
                            return orderMasterRepository.save(newOrder)
                                    .map(savedOrder -> new CustomResponse<>(new Status(201, "Order created successfully"), savedOrder));
                        })
                );
    }

    public Mono<CustomResponse<OrderMaster>> updateOrder(int orderId, OrderMaster updateOrderRequest) {
        return orderMasterRepository.findById(orderId)
                .flatMap(existingOrder -> {
                    existingOrder.setCompanyId(updateOrderRequest.getCompanyId());
                    existingOrder.setMealId(updateOrderRequest.getMealId());
                    existingOrder.setMealCount(updateOrderRequest.getMealCount());
                    existingOrder.setOrderDate(updateOrderRequest.getOrderDate());
                    existingOrder.setDayOfWeek(updateOrderRequest.getDayOfWeek());
//                    existingOrder.setMealBudget(updateOrderRequest.getMealBudget());
                    existingOrder.setMealSuggestionId(updateOrderRequest.getMealSuggestionId());
//                    existingOrder.setMealStartTime(updateOrderRequest.getMealStartTime());
                    existingOrder.setUpdatedBy(updateOrderRequest.getUpdatedBy());
                    existingOrder.setUpdatedTime(Instant.now());

                    return orderMasterRepository.save(existingOrder);
                })
                .map(order -> new CustomResponse<>(new Status(200, "Order updated successfully"), order))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }

    public Mono<OrderMaster> deleteOrder(int orderId) {
        return orderMasterRepository.findById(orderId)
                .flatMap(order -> orderMasterRepository.deleteById(orderId).thenReturn(order))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found..!!"))));
    }

    private CustomResponse<OrderMaster> getNotFoundResponse() {
        return new CustomResponse<>(new Status(404, "Order not found"), null);
    }
}
