package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.data.model.Status;
import com.meteoriqs.foodswing.data.entity.ErrorLogEntity;
import com.meteoriqs.foodswing.data.repository.ErrorLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class ErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    @Autowired
    public ErrorLogService(ErrorLogRepository errorLogRepository){
        this.errorLogRepository = errorLogRepository;
    }

    public Mono<Status> createErrorLog(ErrorLogEntity errorLogEntity){
        ErrorLogEntity errorLog = new ErrorLogEntity();
        errorLog.setError(errorLog.getError());
        errorLog.setCreatedTime(Instant.now());
        errorLog.setModuleName(errorLog.getModuleName());
        return errorLogRepository.save(errorLog)
                .map(log->  new Status(200, "Error log Added successfully"));
    }
}
