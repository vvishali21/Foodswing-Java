package com.meteoriqs.foodswing.controller;

import com.meteoriqs.foodswing.common.mappings.ConfigurationHolder;
import com.meteoriqs.foodswing.data.model.Company;
import com.meteoriqs.foodswing.data.repository.CompanyRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Log4j2
public class TestController {

    private final CompanyRepository companyRepository;

    @Autowired
    public TestController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Mono<ServerResponse> getAllCompany(ServerRequest request) {
        return ServerResponse.ok().body(
                companyRepository.findAll(),
                Company.class
        ).onErrorResume(throwable -> ServerResponse.badRequest().build());
    }
}
