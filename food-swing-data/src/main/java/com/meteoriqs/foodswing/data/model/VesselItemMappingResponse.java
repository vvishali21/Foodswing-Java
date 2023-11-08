package com.meteoriqs.foodswing.data.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class VesselItemMappingResponse extends BaseEntity {

    private int vesselId;
    private int createdBy;
    private String vesselName;
    private int status;
    private BigDecimal capacity;
    private int uomId;
    private String vesselType;
    private BigDecimal maxCapacity;
    private List<Integer> itemIds;
}
