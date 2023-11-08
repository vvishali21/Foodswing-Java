package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class MealSuggestion extends BaseEntity {

    @Id
    private int mealSuggestionId;
    private int sequence;
    private String mealName;
    private BigDecimal mealCost;
    private BigDecimal costRation;
    private int planId;

}
