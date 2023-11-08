package com.meteoriqs.foodswing.data.model;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class VesselIndentResponse {

    private String itemName;
    private String stage;
    private String processName;
    private String mediumName;
    private int ingredientId;
    private String ingredientName;
    private Double quantity;
    private String name;
    private BigDecimal ingredientCost;
    private String stageEndAlertDuration;
    private int processId;
    private int mediumId;
    private int baseSv;
    private Float basePv;
    private int productionSv;
    private Float productPv;
    private String duration;
    private String durationUnit;
    private int power;
    private int fq;
    private String fwdTime;
    private String revTime;
    private String timeTaken;
    private String stageDuration;



}
