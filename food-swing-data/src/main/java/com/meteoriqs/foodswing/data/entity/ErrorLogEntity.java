package com.meteoriqs.foodswing.data.entity;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogEntity {
    @Id
    private int errorLogId;
    private String moduleName;
    private String error;
    private String stackTraceError;
    private Instant createdTime;
}
