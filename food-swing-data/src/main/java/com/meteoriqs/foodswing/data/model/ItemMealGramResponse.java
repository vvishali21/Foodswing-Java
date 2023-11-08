package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemMealGramResponse extends BaseEntity{

    private int itemId;
    private String itemName;
    private int uomId;
    private int categoryId;
    private List<Integer> categoryIds;
    private BigDecimal gram;
    private List<Integer> mealList;
    private BigDecimal weight;

}
