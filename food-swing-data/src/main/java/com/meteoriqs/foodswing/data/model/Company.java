package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Company extends BaseEntity {

    @Id
    private int companyId;
    private String companyName;
    private String primaryContactNumber;
    private String secondaryContactNumber;
    private String email;

    @Transient
    private List<RecipeSearchResponse> costList;
}
