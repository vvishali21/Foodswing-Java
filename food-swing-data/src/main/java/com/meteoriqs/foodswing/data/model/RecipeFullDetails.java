package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeFullDetails extends BaseEntity {
    private int recipeDetailId;
    private int recipeId;
    private int ingredientId;
    private String ingredientName;
    private BigDecimal quantity;
    private int stage;
    private String stageEndAlertDuration;
    private int processId;
    private String processName;
    private int mediumId;
    private String mediumName;
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
    private LocalTime startTime;
    private LocalTime endTime;
    private String timeTaken;
    private String stageDuration;
}