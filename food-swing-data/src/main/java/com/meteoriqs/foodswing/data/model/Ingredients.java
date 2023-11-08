package com.meteoriqs.foodswing.data.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Ingredients extends BaseEntity{

    @Id
    private int ingredientId;
    private String ingredientName;
    private int uomId;

    @Transient
    private String uomName;

    private BigDecimal cost;

    private BigDecimal stock;

    @Transient
    private BigDecimal count;

    @Transient
    private boolean fieldEdit = false;
}
