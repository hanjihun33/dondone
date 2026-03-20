package com.workproofpay.backend.employer;

import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.service.EmployerAccessScope;
import com.workproofpay.backend.employer.service.EmployerAccessScopeService;
import com.workproofpay.backend.shared.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class EmployerAccessScopeServiceTest {

    private final EmployerAccessScopeService service = new EmployerAccessScopeService(mock(), mock(), mock());

    @Test
    void membershipInsideDefaultScopePasses() {
        EmployerAccessScope scope = new EmployerAccessScope(
                1L,
                2L,
                "Acme HR",
                10L,
                "Acme Logistics",
                "ACME",
                100L,
                "Seoul Hub",
                null
        );
        EmploymentMembership membership = EmploymentMembership.create(20L, 10L, 100L, LocalDate.now().minusDays(1));

        assertDoesNotThrow(() -> service.assertMembershipAccessible(scope, membership));
    }

    @Test
    void membershipOutsideDefaultScopeFails() {
        EmployerAccessScope scope = new EmployerAccessScope(
                1L,
                2L,
                "Acme HR",
                10L,
                "Acme Logistics",
                "ACME",
                100L,
                "Seoul Hub",
                null
        );
        EmploymentMembership membership = EmploymentMembership.create(20L, 99L, 100L, LocalDate.now().minusDays(1));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.assertMembershipAccessible(scope, membership));

        assertEquals("FORBIDDEN", exception.getErrorCode().getCode());
    }
}
