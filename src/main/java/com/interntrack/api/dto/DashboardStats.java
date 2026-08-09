package com.interntrack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStats {
    private long total;
    private Map<String, Long> statusCounts;
}
