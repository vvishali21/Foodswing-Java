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
public class MealItemMapping extends BaseEntity {
    @Id
    private int id;

    private int mealId;
    @Transient
    private String mealName;

    private int itemId;
    @Transient
    private String itemName;

    private int categoryId;
    @Transient
    private String categoryName;

    @Transient
    private BigDecimal plannedQuantity;
}
