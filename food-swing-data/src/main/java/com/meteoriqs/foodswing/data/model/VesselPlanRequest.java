package com.meteoriqs.foodswing.data.model;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class VesselPlanRequest {

    private List<Integer> vesselIds;
    private int mealSuggestionId;
}
