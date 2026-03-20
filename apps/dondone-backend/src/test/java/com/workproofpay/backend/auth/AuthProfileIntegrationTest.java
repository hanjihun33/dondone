package com.workproofpay.backend.auth;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthProfileIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void signupLoginAndMeIncludePhoneNumber() throws Exception {
        String signupJson = """
                {
                  "email": "phone-auth@test.com",
                  "password": "password123",
                  "name": "Phone Auth",
                  "phoneNumber": "010-9876-5432"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("phone-auth@test.com"))
                .andExpect(jsonPath("$.data.phoneNumber").value("01098765432"));

        String loginJson = """
                {
                  "email": "phone-auth@test.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneNumber").value("01098765432"));

        User user = userRepository.findByEmailIgnoreCase("phone-auth@test.com").orElseThrow();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(tokenFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneNumber").value("01098765432"));
    }

    @Test
    void updateMeUpdatesNameAndPhoneNumber() throws Exception {
        User user = userRepository.save(User.register("profile@test.com", "hashed", "Before", "01011112222"));

        String updateJson = """
                {
                  "name": "After",
                  "phoneNumber": "010-3333-4444"
                }
                """;

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", bearer(tokenFor(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("After"))
                .andExpect(jsonPath("$.data.phoneNumber").value("01033334444"));
    }

    @Test
    void signupRejectsDuplicatePhoneNumber() throws Exception {
        userRepository.save(User.register("first@test.com", "hashed", "First", "01055556666"));

        String signupJson = """
                {
                  "email": "second@test.com",
                  "password": "password123",
                  "name": "Second",
                  "phoneNumber": "010-5555-6666"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_NUMBER_ALREADY_EXISTS"));
    }

    @Test
    void updateMeRejectsDuplicatePhoneNumber() throws Exception {
        User user = userRepository.save(User.register("profile-a@test.com", "hashed", "Profile A", "01011112222"));
        userRepository.save(User.register("profile-b@test.com", "hashed", "Profile B", "01033334444"));

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", bearer(tokenFor(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "After",
                                  "phoneNumber": "010-3333-4444"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_NUMBER_ALREADY_EXISTS"));
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
