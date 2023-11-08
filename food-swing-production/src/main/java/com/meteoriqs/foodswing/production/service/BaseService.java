package com.meteoriqs.foodswing.production.service;

import com.meteoriqs.foodswing.common.model.CustomPaginateResponse;
import com.meteoriqs.foodswing.data.model.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseService {

    <T> CustomPaginateResponse<List<T>> getErrorResponse(int code, String message) {
        return new CustomPaginateResponse<>(new Status(code, message), null, null);
    }

}
