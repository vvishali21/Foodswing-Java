package com.meteoriqs.foodswing.data.repository;

import com.meteoriqs.foodswing.data.model.SequenceMaster;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface SequenceMasterRepository extends ReactiveCrudRepository <SequenceMaster,Integer> {

    Mono<SequenceMaster> findByPrefixEquals(String prefix);

}
