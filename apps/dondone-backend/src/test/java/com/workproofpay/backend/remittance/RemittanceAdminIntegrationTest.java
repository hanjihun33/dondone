package com.workproofpay.backend.remittance;

import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RemittanceAdminIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void blocksNonAdminAndAllowsAdminOnOpsSummary() throws Exception {
        String userToken = jwtTokenProvider.createAccessToken(1L, "user@test.com", "USER");
        String adminToken = jwtTokenProvider.createAccessToken(999L, "admin@test.com", "ADMIN");

        mockMvc.perform(get("/api/admin/remittance/summary")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/admin/remittance/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transferCounts.REQUESTED").exists())
                .andExpect(jsonPath("$.data.jobCounts.FAILED").exists())
                .andExpect(jsonPath("$.data.walletFundingCounts.FAILED").exists());
    }
}
