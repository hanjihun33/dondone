package com.workproofpay.backend.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DonDone API")
                        .description("""
                                DonDone backend API documentation.

                                Swagger auth test flow:
                                1. Call `POST /api/auth/login` with the seed account.
                                2. Copy `data.accessToken` from the response.
                                3. Click `Authorize` and paste `Bearer {token}` or the raw token value.
                                4. Call protected endpoints such as `GET /api/auth/me`.

                                Remittance quick test flow:
                                1. Call `POST /api/remittance/wallets/me`.
                                2. Register a recipient with `POST /api/remittance/recipients`.
                                3. Run `POST /api/remittance/transfers/precheck`.
                                4. Create a transfer with `POST /api/remittance/transfers` and `Idempotency-Key`.
                                5. Poll `GET /api/remittance/transfers/{transferId}` until `CONFIRMED`.
                                """)
                        .version("v0.1.0")
                        .contact(new Contact().name("DonDone Backend")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT access token. `Bearer` prefix is optional in Swagger UI.")));
    }
}
