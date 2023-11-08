package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponse {
    private Status status;
    private List<RecipeDetails> data;
    private PaginationInfo paginationInfo;
}
