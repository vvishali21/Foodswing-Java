package com.meteoriqs.foodswing.data.model;

import com.meteoriqs.foodswing.data.entity.RecipeMaster;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeWithItem {
    private RecipeMaster recipeMaster;
    private Item item;
}
