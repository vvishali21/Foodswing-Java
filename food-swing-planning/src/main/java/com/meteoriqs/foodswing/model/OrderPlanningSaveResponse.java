package com.meteoriqs.foodswing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlanningSaveResponse {

    private int mealSuggestionId;
    private List<Integer> orderIds;

}
