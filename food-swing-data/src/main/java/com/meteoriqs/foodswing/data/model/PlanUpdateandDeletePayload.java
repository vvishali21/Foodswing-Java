package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanUpdateandDeletePayload {
    private List<Integer> deleted;
    private List<OrderMaster> orders;
    private SuggestionQuantityUpdateRequest mealSuggestionPlanUpdate;
}
