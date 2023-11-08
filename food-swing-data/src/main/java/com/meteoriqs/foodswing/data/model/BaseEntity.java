package com.meteoriqs.foodswing.data.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Data
public abstract class BaseEntity {
//    TODO: Set default for active as true for all entities in DB
//    TODO: Check @CreatedBy and @LastModifiedBy to set in BaseEntity
    private boolean isActive;
    private int createdBy;
    @CreatedDate
    private Instant createdTime;
    private int updatedBy;
    @LastModifiedDate
    private Instant updatedTime;
}
