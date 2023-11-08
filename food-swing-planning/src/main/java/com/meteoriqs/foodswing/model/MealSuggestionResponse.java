package com.meteoriqs.foodswing.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class MealSuggestionResponse {

    private int mealId;
    private List<String> itemsWithoutRecipe;
    private List<MenuSuggestionResponse> menuSuggestion;
}

