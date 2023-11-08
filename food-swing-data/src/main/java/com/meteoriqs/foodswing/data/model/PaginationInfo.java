package com.meteoriqs.foodswing.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationInfo {
    private long totalCount;
    private int currentPage;
    private int pageSize;
}
