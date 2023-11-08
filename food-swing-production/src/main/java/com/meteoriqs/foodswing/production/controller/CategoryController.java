package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.model.Category;
import com.meteoriqs.foodswing.production.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
@Component
public class CategoryController extends BaseController{

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public Mono<ServerResponse> getAllCategory(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return categoryService.getAllCategory(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getCategoryById(ServerRequest request) {
        int categoryId = Integer.parseInt(request.pathVariable("categoryId"));

        return categoryService.getCategoryById(categoryId)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> createCategory(ServerRequest request) {
        Mono<Category> createCategoryRequestMono = request.bodyToMono(Category.class);

        return createCategoryRequestMono
                .flatMap(categoryService::createCategory)
                .flatMap(createdCategory -> ServerResponse.status(HttpStatus.CREATED).bodyValue(createdCategory))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("Request body is empty..!!"))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateCategory(ServerRequest request) {
        int categoryId = Integer.parseInt(request.pathVariable("categoryId"));
        Mono<Category> updateCategoryRequestMono = request.bodyToMono(Category.class);

        return updateCategoryRequestMono.flatMap(updateCategoryRequest -> categoryService.updateCategory(categoryId, updateCategoryRequest))
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> deleteCategory(ServerRequest request) {
        int categoryId = Integer.parseInt(request.pathVariable("categoryId"));

        return categoryService.deleteCategory(categoryId)
                .flatMap(deletedCategory -> ServerResponse.ok().bodyValue("Category deleted successfully..!!"))
                .onErrorResume(this::handleError);
    }
}
