package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.Company;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CompanyRepository extends ReactiveCrudRepository<Company, Integer> {

    @Query("SELECT * FROM company ORDER BY company_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<Company> findAllWithPagination (Pageable pageable);

    Mono<Company> findByCompanyName(String companyName);
}
