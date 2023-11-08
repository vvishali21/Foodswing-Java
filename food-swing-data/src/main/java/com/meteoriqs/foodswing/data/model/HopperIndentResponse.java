package com.meteoriqs.foodswing.data.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HopperIndentResponse {

    private String itemName;
    private int ingredientId;
    private String ingredientName;
    private Double quantity;
    private String name;

}
