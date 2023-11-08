package com.meteoriqs.foodswing.data.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@Getter
@Setter
public class CombinedResponse {

    private Status status;
    private List<String> noVesselAvailableForItem;
    private VesselPlanningResponse vesselPlanning;
    private IngredientsResponse ingredients;


    public CombinedResponse(Status status, VesselPlanningResponse vesselPlanning, IngredientsResponse ingredients) {
        this.status = status;
        this.noVesselAvailableForItem = new ArrayList<>();
        this.vesselPlanning = vesselPlanning;
        this.ingredients = ingredients;
    }
}


