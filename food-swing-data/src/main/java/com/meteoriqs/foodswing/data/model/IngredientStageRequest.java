package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientStageRequest extends BaseEntity {
    private int ingredientId;
    private BigDecimal quantity;
    private int mediumId;
}
