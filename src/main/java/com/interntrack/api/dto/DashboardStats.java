package com.interntrack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class DashboardStats {
    private long total;
    private Map<String, Long> statusCounts;
}
