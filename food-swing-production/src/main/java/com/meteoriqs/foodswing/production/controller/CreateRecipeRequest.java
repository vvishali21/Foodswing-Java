package com.meteoriqs.foodswing.production.controller;

import com.meteoriqs.foodswing.data.entity.RecipeMaster;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecipeRequest  {
    private String recipeName;
    private int itemId;
    private int preparationQuantity;
    private String recipeDescription;
    private String totalTimeTaken;
    private RecipeMaster recipeMaster;
}
