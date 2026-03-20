package com.workproofpay.backend.employer.api.dto.request;

import com.workproofpay.backend.employer.api.dto.response.EmployerWorkerAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class EmployerWorkersQuery {

    @Schema(description = "Case-insensitive keyword matched against worker name or email", example = "kim")
    private String query;

    @Schema(description = "Attendance statuses to include. Repeated query parameters are supported")
    private List<EmployerWorkerAttendanceStatus> statuses = new ArrayList<>();

    @Schema(description = "1-based page number", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    private Integer size = 20;
}
