package com.meteoriqs.foodswing.data.model;

import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
public class HopperCategoriesResponse {

    private final String itemName;
    private final List<HopperIndentResponse> ingredients;

    public HopperCategoriesResponse(String itemName, List<HopperIndentResponse> ingredients) {
        this.itemName = itemName;
        this.ingredients = ingredients;
    }

    public String getItemName() {
        return itemName;
    }

    public List<HopperIndentResponse> getIngredients() {
        return ingredients;
    }
}
