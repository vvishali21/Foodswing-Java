package com.meteoriqs.foodswing.data.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
@Data
@NoArgsConstructor
@Getter
@Setter
public class VesselPlanningResponse {

    private Map<String, List<Map<String, Object>>> data;

    public VesselPlanningResponse(Map<String, List<Map<String, Object>>> data) {
        this.data = data;
    }
}
