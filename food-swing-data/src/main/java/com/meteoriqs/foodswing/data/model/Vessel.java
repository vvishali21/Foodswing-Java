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
public class Vessel extends BaseEntity{

    @Id
    private int vesselId;
    private String vesselName;
    private int status;
    private BigDecimal capacity;
    private int uomId;
    private String vesselType;
}
