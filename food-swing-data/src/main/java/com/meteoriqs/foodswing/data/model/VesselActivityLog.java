package com.meteoriqs.foodswing.data.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class VesselActivityLog extends BaseEntity{

    @Id
    private int activityId;
    private int vesselId;
    private int runningStatus;
    private int orderId;
    private int itemId;
    private String remarks;
}
