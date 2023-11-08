package com.meteoriqs.foodswing.data.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HopperIndentItemResponse {

    private String itemName;
    private int ingredientId;
    private String ingredientName;
    private int mediumId;
    private String mediumName;
    private Double quantity;
    private String name;
}
