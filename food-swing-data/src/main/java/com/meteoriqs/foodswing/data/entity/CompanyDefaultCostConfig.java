package com.meteoriqs.foodswing.data.entity;

import com.meteoriqs.foodswing.data.model.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class CompanyDefaultCostConfig extends BaseEntity {
    @Id
    private int id;
    private int companyId;
    private int mealId;
    private BigDecimal defaultCost;
}
