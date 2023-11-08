package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDetailsResponse extends BaseEntity {
    private int itemId;
    private String itemName;
    private int uomId;
    private String uomName;
}
