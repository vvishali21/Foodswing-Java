package com.meteoriqs.foodswing.data.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class MealProductionPlan{

    @Id
    private int id;
    private int planId;
    private int mealSuggestionId;
    private int itemId;
    private int vesselId;
    private int recipeId;
}
