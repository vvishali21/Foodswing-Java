package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionQuantityUpdateRequest {
        private int mealSuggestionId;
        private int dayOfWeek;
        private int mealId;
        private List<CompanyCountPayload> companyDataList;
}
