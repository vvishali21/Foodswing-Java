package com.meteoriqs.foodswing.data.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Item extends BaseEntity{
        @Id
        private int itemId;
        private String itemName;
        private int uomId;
        private int categoryId;
        private BigDecimal gram;
        private BigDecimal weight;
}
