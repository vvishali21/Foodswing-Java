package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.data.model.*;
import com.meteoriqs.foodswing.data.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class MealItemMappingService extends BaseService {

    private final MealItemMappingRepository mealItemMappingRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final MealRepository mealRepository;

    public MealItemMappingService(MealItemMappingRepository mealItemMappingRepository, MealRepository mealRepository,
                                  ItemRepository itemRepository, CategoryRepository categoryRepository) {
        this.mealItemMappingRepository = mealItemMappingRepository;
        this.mealRepository = mealRepository;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }


    public Mono<CustomPaginateResponse<List<MealItemMapping>>> getAllMealItem(Pageable pageable) {
        return mealItemMappingRepository.count()
                .flatMap(totalCount -> mealItemMappingRepository.findAllWithPagination(pageable)
                        .flatMap(mapping -> {
                            Mono<Meal> mealMono = mealRepository.findById(mapping.getMealId());
                            Mono<Item> itemMono = itemRepository.findById(mapping.getItemId());
                            Mono<Category> categoryMono = categoryRepository.findById(mapping.getCategoryId());

                            return Mono.zip(mealMono, itemMono, categoryMono)
                                    .map(tuple -> {
                                        Meal meal = tuple.getT1();
                                        Item item = tuple.getT2();
                                        Category category = tuple.getT3();

                                        mapping.setMealName(meal.getName());
                                        mapping.setItemName(item.getItemName());
                                        mapping.setCategoryName(category.getCategoryName());

                                        return mapping;
                                    });
                        })
                        .collectList()
                        .map(paginatedMealItems -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount,
                                    pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<MealItemMapping>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedMealItems);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All MealItem Error Occurred")));
    }
}
