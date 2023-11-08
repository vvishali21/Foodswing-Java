package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSearchResponse implements Serializable {
    private int id;
    private String name;
    private int uomId;
    private String uomName;
    private BigDecimal cost;
}
