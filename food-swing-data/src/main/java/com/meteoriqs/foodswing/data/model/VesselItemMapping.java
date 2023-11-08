package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("vessel_item_mapping")
public class VesselItemMapping extends BaseEntity{

    @Id
    private int id;

    @Column("vessel_id")
    private int vesselId;

    @Column("item_id")
    private int itemId;

    @Column("max_capacity")
    private BigDecimal maxCapacity;

    @Transient
    private boolean fieldEdit = false;

    @Transient
    private String itemName; // Field to store item name

    @Transient
    private String vesselName;

}
