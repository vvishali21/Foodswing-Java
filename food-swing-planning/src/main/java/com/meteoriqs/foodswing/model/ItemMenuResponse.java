package com.meteoriqs.foodswing.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ItemMenuResponse {

    @Id
    private int itemId;
    private String itemName;
    private BigDecimal plannedQuantity;
    private int categoryId;

    public ItemMenuResponse(int itemId, String itemName, BigDecimal plannedQuantity, int categoryId) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.plannedQuantity = plannedQuantity;
        this.categoryId = categoryId;
    }

}
