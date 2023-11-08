package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.entity.CompanyDefaultCostConfig;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CompanyCostConfigRepository extends ReactiveCrudRepository<CompanyDefaultCostConfig, Integer> {
    Flux<CompanyDefaultCostConfig> findByCompanyId(int companyId);
}
