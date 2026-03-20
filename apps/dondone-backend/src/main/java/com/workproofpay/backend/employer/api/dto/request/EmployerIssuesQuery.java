package com.workproofpay.backend.employer.api.dto.request;

import com.workproofpay.backend.employer.api.dto.response.EmployerIssueItemType;

import java.util.List;

public record EmployerIssuesQuery(
        String query,
        List<EmployerIssueItemType> itemTypes,
        Integer page,
        Integer size
) {
}
