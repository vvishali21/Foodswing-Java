package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VesselIngredientsIndent {


    private int mediumId;
    private String mediumName;
    private int ingredientId;
    private String ingredientName;
    private BigDecimal ingredientCost;
    private double quantity;
    private String name;
    private double basePv;
    private int baseSv;
    private String stageEndAlertDuration;
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
