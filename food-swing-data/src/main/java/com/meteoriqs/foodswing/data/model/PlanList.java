package com.meteoriqs.foodswing.data.model;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanList {
    private int mealSuggestionId;
    private int planId;
    private String companyIds;
    private List<String> companyNames;
    private int mealCount;
    private int mealId;
    private String name;
    private int dayOfWeek;
    private int status;
    private LocalDate orderDate;
}
