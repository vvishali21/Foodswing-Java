package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.entity.CompanyMealCostMapping;
import com.meteoriqs.foodswing.data.model.Company;
import com.meteoriqs.foodswing.data.model.Grammage;
import com.meteoriqs.foodswing.production.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CompanyController extends BaseController{

    private final CompanyService companyService;

    @Autowired
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    public Mono<ServerResponse> getAllCompany(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return companyService.getAllCompany(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getAllCompanyCost(ServerRequest request) {
        Pageable pageable = getPageableInfo(request);

        return companyService.getAllCompanyCostMapping(pageable)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getCompanyById(ServerRequest request) {
        int companyId = Integer.parseInt(request.pathVariable("companyId"));

        return companyService.getCompanyById(companyId)
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> createCompany(ServerRequest request) {
        Mono<Company> createCompanyRequestMono = request.bodyToMono(Company.class);

        return createCompanyRequestMono
                .flatMap(companyService::createCompany)
                .flatMap(createdCompany -> ServerResponse.status(HttpStatus.CREATED).bodyValue(createdCompany))
                .switchIfEmpty(ServerResponse.badRequest().bodyValue("Request body is empty..!!"))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateCompany(ServerRequest request) {
        int companyId = Integer.parseInt(request.pathVariable("companyId"));
        Mono<Company> updateCompanyRequestMono = request.bodyToMono(Company.class);

        return updateCompanyRequestMono.flatMap(updateCompanyRequest -> companyService.updateCompany(companyId, updateCompanyRequest))
                .flatMap(this::okResponse)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> updateCompanyCost(ServerRequest request) {
        Flux<CompanyMealCostMapping> updateCompanyRequestFlux = request.bodyToFlux(CompanyMealCostMapping.class);

        return companyService.updateCompanyCost(updateCompanyRequestFlux)
                .flatMap(customResponse -> {
                    if (customResponse.getStatus().getCode() == 200) {
                        return ServerResponse.ok().bodyValue(customResponse);
                    } else {
                        return ServerResponse.ok().bodyValue(customResponse);
                    }
                })
                .onErrorResume(e -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    public Mono<ServerResponse> deleteCompany(ServerRequest request) {
        int companyId = Integer.parseInt(request.pathVariable("companyId"));

        return companyService.deleteCompany(companyId)
                .flatMap(deletedCompany -> ServerResponse.ok().bodyValue("Company deleted successfully..!!"))
                .onErrorResume(this::handleError);
    }
}
