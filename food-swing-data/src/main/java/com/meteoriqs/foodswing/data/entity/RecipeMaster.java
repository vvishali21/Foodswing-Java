package com.meteoriqs.foodswing.data.entity;

import com.meteoriqs.foodswing.data.model.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recipe_master")
public class RecipeMaster extends BaseEntity {

    @Id
    private int recipeId;
    private int itemId;
    private int uomId;
    private int preparationQuantity;
    private String recipeDescription;
    private String totalTimeTaken;
    private String preparationType;
}
