package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.entity.ErrorLogEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ErrorLogRepository extends ReactiveCrudRepository<ErrorLogEntity,Integer> {
}
