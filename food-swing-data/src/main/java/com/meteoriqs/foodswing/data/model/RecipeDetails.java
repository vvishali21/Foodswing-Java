package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDetails extends BaseEntity{

    private int recipeId;
    private int preparationQuantity;
    private BigDecimal recipeCost;
    private String recipeDescription;
    private String totalTimeTaken;
    private ItemDetailsResponse itemDetailsResponse;
    private String preparationType;
}
