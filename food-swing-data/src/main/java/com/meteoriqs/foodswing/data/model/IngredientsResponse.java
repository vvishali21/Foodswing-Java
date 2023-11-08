package com.meteoriqs.foodswing.data.model;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@Getter
@Setter
public class IngredientsResponse {
    private List<ValidatePlanIndentResponse> data;

    public IngredientsResponse(List<ValidatePlanIndentResponse> data) {
        this.data = data;
    }
}
