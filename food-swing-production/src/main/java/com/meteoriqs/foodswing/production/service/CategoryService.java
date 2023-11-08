package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.common.model.CustomResponse;
import com.meteoriqs.foodswing.data.model.Category;
import com.meteoriqs.foodswing.data.model.PaginationInfo;
import com.meteoriqs.foodswing.data.model.RecipeSearchResponse;
import com.meteoriqs.foodswing.data.model.Status;
import com.meteoriqs.foodswing.data.repository.CategoryRepository;
import net.spy.memcached.MemcachedClient;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class CategoryService extends BaseService {

    public static final String CATEGORY_NAMES = "categoryNames-";
    private final CategoryRepository categoryRepository;

    private final MemcachedClient memcachedClient;

    public CategoryService(CategoryRepository categoryRepository, MemcachedClient memcachedClient) {
        this.categoryRepository = categoryRepository;
        this.memcachedClient = memcachedClient;
    }

    public Mono<CustomPaginateResponse<List<Category>>> getAllCategory(Pageable pageable) {

        return categoryRepository.count()
                .flatMap(totalCount -> categoryRepository.findAllWithPagination(pageable)
                        .collectList()
                        .map(paginatedCategory -> {
                            PaginationInfo paginationInfo = new PaginationInfo(totalCount, pageable.getPageNumber() + 1, pageable.getPageSize());

                            CustomPaginateResponse<List<Category>> response = new CustomPaginateResponse<>();
                            response.setStatus(new Status(200, "Success"));
                            response.setData(paginatedCategory);
                            response.setPaginationInfo(paginationInfo);
                            return response;
                        })
                        .onErrorReturn(getErrorResponse(500, "Get All Category Error Occurred")));
    }


    public Mono<CustomResponse<Category>> getCategoryById(int categoryId) {
        return categoryRepository.findById(categoryId)
                .map(category -> new CustomResponse<>(new Status(200, "Success"), category))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }

    public Mono<CustomResponse<Object>> createCategory(Category createCategoryRequest) {
        return categoryRepository.findByCategoryName(createCategoryRequest.getCategoryName())
                .flatMap(existingCategory -> Mono.just(new CustomResponse<>(new Status(403, "Category name already exists"), null)))
                .switchIfEmpty(Mono.defer(() -> {
                    Category newCategory = new Category();
                    newCategory.setCategoryName(createCategoryRequest.getCategoryName());
                    newCategory.setActive(true);
                    newCategory.setCreatedBy(createCategoryRequest.getCreatedBy());
                    newCategory.setCreatedTime(Instant.now());
                    return categoryRepository.save(newCategory)
                            .flatMap(savedCategory -> {
                                memcachedClient.set(CATEGORY_NAMES +savedCategory.getCategoryId(),0,savedCategory.getCategoryName());

                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("categoryEntityList");
                                processData.add(new RecipeSearchResponse(savedCategory.getCategoryId(),savedCategory.getCategoryName(),0,null,null));
                                memcachedClient.set("categoryEntityList",0,processData);

                                return Mono.just(new CustomResponse<>(new Status(201, "Category created successfully"), savedCategory));
                            });
                }));
    }


    public Mono<CustomResponse<Category>> updateCategory(int categoryId, Category updateCategoryRequest) {
        return categoryRepository.findById(categoryId)
                .flatMap(existingCategory -> {
                    existingCategory.setCategoryName(updateCategoryRequest.getCategoryName());
                    existingCategory.setUpdatedBy(updateCategoryRequest.getUpdatedBy());
                    existingCategory.setUpdatedTime(Instant.now());

                    return categoryRepository.save(existingCategory)
                            .doOnNext(savedCategory ->{
                                    memcachedClient.set(CATEGORY_NAMES +savedCategory.getCategoryId(),0,savedCategory.getCategoryName());
                                List<RecipeSearchResponse> processData = (List<RecipeSearchResponse>) memcachedClient.get("categoryEntityList");
                                processData.add(new RecipeSearchResponse(savedCategory.getCategoryId(),savedCategory.getCategoryName(),0,null,null));
                                memcachedClient.set("categoryEntityList",0,processData);

                            });
                })
                .map(category -> new CustomResponse<>(new Status(200, "Category updated successfully"), category))
                .switchIfEmpty(Mono.just(getNotFoundResponse()));
    }

    public Mono<Category> deleteCategory(int categoryId) {
        return categoryRepository.findById(categoryId)
                .flatMap(category -> categoryRepository.deleteById(categoryId).thenReturn(category))
                .doOnNext(deletedCategory -> memcachedClient.delete(CATEGORY_NAMES +categoryId))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found..!!"))));
    }

    private CustomResponse<Category> getNotFoundResponse() {
        return new CustomResponse<>(new Status(404, "Category not found"), null);
    }

}
