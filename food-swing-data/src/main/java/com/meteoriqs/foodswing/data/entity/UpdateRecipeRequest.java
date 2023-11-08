package com.meteoriqs.foodswing.data.entity;

import com.meteoriqs.foodswing.data.model.RecipeStageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecipeRequest {
    private String isDraft;
    private RecipeMaster recipeMasterRequest;
    private List<RecipeStageRequest> recipeDetailsEntityRequest;
}
