package com.meteoriqs.foodswing.data.model;

import com.meteoriqs.foodswing.data.entity.RecipeDetailsEntity;
import com.meteoriqs.foodswing.data.entity.RecipeMaster;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRequest {
    private RecipeMaster recipeMasterRequest;
    private List<RecipeDetailsEntity> recipeDetailsEntityRequest;
}
