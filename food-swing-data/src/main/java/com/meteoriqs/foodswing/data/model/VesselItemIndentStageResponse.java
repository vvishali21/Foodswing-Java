package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VesselItemIndentStageResponse {

    private String stage;
    private String itemName;
    private String processName;
    private List<String> vesselIds;
    private List<String> vesselNames;
}
