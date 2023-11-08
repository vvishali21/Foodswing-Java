package com.meteoriqs.foodswing.data.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class MealSuggestionDetails extends BaseEntity{

    @Id
    private int mealSuggestionDetailsId;
    private int mealSuggestionId;
    private int itemId;

    @Transient
    private String itemName;

    @Transient
    private BigDecimal totalCost;

    @Transient
    private BigDecimal perMealCost;

    @Transient
    private BigDecimal unitRate;

    @Transient
    private String contribution;

    private int recipeId;
    private BigDecimal grammage;
    private BigDecimal quantity;
    private int uomId;
    private BigDecimal itemCost;
    private BigDecimal itemWeightage;

}
