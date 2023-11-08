package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemWithDetailsDTO {

    private int itemId;
    private String itemName;
    private String uomName;
    private String gram;
    private BigDecimal weight;
    private String categoryName;
    private List<String> mealName;
}
