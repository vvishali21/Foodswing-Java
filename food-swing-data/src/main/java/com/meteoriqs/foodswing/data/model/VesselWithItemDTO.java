package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VesselWithItemDTO {

    private int vesselId;
    private String vesselName;
    private int status;
    private BigDecimal capacity;
    private List<String> itemNames;
}
