package com.meteoriqs.foodswing.data.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Data
@Getter
@Setter
public class HopperCategoriesItemResponse {

    private final String itemName;
    private final List<HopperIndentItemResponse> ingredients;

    public HopperCategoriesItemResponse(String itemName, List<HopperIndentItemResponse> ingredients) {
        this.itemName = itemName;
        this.ingredients = ingredients;
    }

    public String getItemName() {
        return itemName;
    }

    public List<HopperIndentItemResponse> getIngredients() {
        return ingredients;
    }
}
