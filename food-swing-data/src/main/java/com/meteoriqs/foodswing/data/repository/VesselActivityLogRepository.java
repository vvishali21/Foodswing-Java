package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.VesselActivityLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VesselActivityLogRepository extends ReactiveCrudRepository<VesselActivityLog, Integer> {
}
