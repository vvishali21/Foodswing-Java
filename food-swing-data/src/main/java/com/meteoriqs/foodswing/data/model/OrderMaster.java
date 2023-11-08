package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class OrderMaster extends BaseEntity{

    @Id
    private int orderId;
    private int companyId;

    @Transient
    private String companyName;

    private int mealId;

    @Transient
    private String mealName;

    @Transient
    private boolean fieldEdit = false;

    private int mealCount;
    private LocalDate orderDate;
    private int dayOfWeek;
    private BigDecimal mealBudget;
    private int mealSuggestionId;
    private Instant mealStartTime;
    private int status;

}
