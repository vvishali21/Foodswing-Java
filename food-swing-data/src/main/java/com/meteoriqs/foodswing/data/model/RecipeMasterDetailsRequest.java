package com.meteoriqs.foodswing.data.model;

import com.meteoriqs.foodswing.data.entity.RecipeMaster;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeMasterDetailsRequest extends BaseEntity{
    private String isDraft;
    private RecipeMaster recipeMasterRequest;
    private List<RecipeStageRequest> recipeDetailsEntityRequest = new ArrayList<>();
}
