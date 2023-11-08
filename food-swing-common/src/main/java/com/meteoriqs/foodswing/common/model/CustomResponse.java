package com.meteoriqs.foodswing.common.model;

import com.meteoriqs.foodswing.data.model.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomResponse<T> {
    private Status status;
    private T data;
}
