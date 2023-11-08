package com.meteoriqs.foodswing.model;

import com.meteoriqs.foodswing.data.model.RecipeGetResponseModel;
import com.meteoriqs.foodswing.data.model.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponseGetModelPlanning {
    private Status status;
    private RecipeGetResponseModel.RecipeGetDetailsResponse data;
}
