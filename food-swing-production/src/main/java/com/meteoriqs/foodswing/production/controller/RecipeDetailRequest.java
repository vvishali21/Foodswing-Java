package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.entity.RecipeDetailsEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDetailRequest {
    private RecipeDetailsEntity recipeDetailsEntity;
    private int ingredientId;
    private BigDecimal quantity;
    private int stage;
    private int stageEndAlertDuration;
    private int processId;
    private int mediumId;
    private double basePv;
    private double baseSv;
    private double productionSv;
    private double productPv;
    private String duration;
    private String durationUnit;
    private double power;
    private int fq;
    private String fwdTime;
    private String revTime;
    private String startTime;
    private String endTime;
    private String timeTaken;

}
