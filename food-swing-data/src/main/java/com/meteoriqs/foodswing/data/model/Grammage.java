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
@Table(name = "grammage")
public class Grammage extends BaseEntity{

        @Id
        private int grammageId;
        private int companyId;
        private int mealId;
        private int itemId;
        private int day;
        private BigDecimal gram;

        @Transient
        private boolean fieldEdit = false;
}
