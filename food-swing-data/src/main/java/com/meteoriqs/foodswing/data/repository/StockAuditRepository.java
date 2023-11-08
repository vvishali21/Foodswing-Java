package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.entity.StockAudit;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockAuditRepository extends ReactiveCrudRepository<StockAudit, Integer> {
}
