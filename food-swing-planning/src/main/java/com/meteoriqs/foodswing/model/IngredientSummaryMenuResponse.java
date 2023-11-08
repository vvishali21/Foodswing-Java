package com.meteoriqs.foodswing.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
@Data
@AllArgsConstructor
public class IngredientSummaryMenuResponse {

    private List<ItemIngredient> details;
    @Data
    @AllArgsConstructor
    public static class ItemIngredient {
        private BigDecimal costPerMeal;
        private BigDecimal totalItemWeight;
        private List<Map<String, Object>> items;
        private List<Map<String, Object>> ingredients;
    }
}
