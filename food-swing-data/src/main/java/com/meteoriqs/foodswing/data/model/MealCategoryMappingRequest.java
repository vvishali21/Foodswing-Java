package com.meteoriqs.foodswing.data.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MealCategoryMappingRequest extends BaseEntity{

    private int mealId;
    private String name;
    private int createdBy;
    private List<RecipeSearchResponse> categoryIds;
}
