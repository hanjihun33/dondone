package com.workproofpay.backend.employer.api.dto.request;

import com.workproofpay.backend.employer.api.dto.response.EmployerIssueItemType;
import com.workproofpay.backend.employer.api.dto.response.EmployerIssueStatus;

import java.util.List;

public record EmployerIssuesQuery(
        String query,
        List<EmployerIssueItemType> itemTypes,
        List<EmployerIssueStatus> statuses,
        Integer page,
        Integer size
) {
}
