package com.meteoriqs.foodswing.data.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeStageRequest {

    private VesselIndentIngredientsResponse vesselData = new VesselIndentIngredientsResponse();
    private List<IngredientStageRequest> ingredientSteps = new ArrayList<>();

}
