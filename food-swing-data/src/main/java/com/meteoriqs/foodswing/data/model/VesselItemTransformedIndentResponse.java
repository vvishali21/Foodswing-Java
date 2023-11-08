package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VesselItemTransformedIndentResponse {

    private VesselItemIndentStageResponse stage;
    private List<VesselIngredientsIndent> ingredients;
}
