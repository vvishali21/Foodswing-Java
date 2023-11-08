package com.meteoriqs.foodswing.data.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ValidatePlanIndentResponse {

    private int ingredientId;
    private String ingredientName;
    private Double quantity;
    private String name;
}
