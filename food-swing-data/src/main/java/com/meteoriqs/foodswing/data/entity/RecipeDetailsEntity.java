package com.meteoriqs.foodswing.data.entity;

import com.meteoriqs.foodswing.data.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recipe_details")
public class RecipeDetailsEntity extends BaseEntity {

    @Id
    private int recipeDetailId;
    private int recipeId;
    private int ingredientId;
    private BigDecimal ingredientCost;
    private BigDecimal quantity;
    private int stage;
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
    private LocalTime startTime;
    private LocalTime endTime;
    private String timeTaken;
    private String stageDuration;
}
