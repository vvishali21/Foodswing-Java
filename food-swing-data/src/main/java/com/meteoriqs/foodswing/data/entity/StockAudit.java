package com.meteoriqs.foodswing.data.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class StockAudit {

    @Id
    private int id;
    private int ingredientId;
    private BigDecimal ingredientQty;
    private BigDecimal usedQty;
    private BigDecimal wastageQty;
    private BigDecimal price;
    private int createdBy;
    private Instant createdTime;
}
