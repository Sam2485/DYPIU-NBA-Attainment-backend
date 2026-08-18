package com.dypiu.nba.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationProfileTest {

    @Test
    @DisplayName("Production profile fails fast when required secrets are missing")
    void testProductionProfileFailsFastWithoutSecrets() {
        assertThrows(Exception.class, () -> {
            new SpringApplicationBuilder(com.dypiu.nba.ObeBackendApplication.class)
                    .profiles("prod")
                    .properties(
                            "spring.datasource.url=jdbc:postgresql://localhost:5432/dypiu_obe_db",
                            "spring.datasource.username=testuser"
                            // Intentionally omitting DATABASE_PASSWORD, JWT_SECRET, CORS_ALLOWED_ORIGINS
                    )
                    .run();
        });
    }

    @Test
    @DisplayName("Production profile loads successfully when all required secrets are provided via environment properties")
    void testProductionProfileLoadsWithInjectedSecrets() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(com.dypiu.nba.ObeBackendApplication.class)
                .profiles("prod")
                .web(org.springframework.boot.WebApplicationType.NONE)
                .properties(
                        "DATABASE_URL=jdbc:h2:mem:prodtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                        "DATABASE_USERNAME=testuser",
                        "DATABASE_PASSWORD=TestProdPass123!",
                        "DATABASE_DRIVER=org.h2.Driver",
                        "JWT_SECRET=test_production_super_secret_jwt_key_that_is_at_least_256_bits_long_for_hmac_sha",
                        "CORS_ALLOWED_ORIGINS=https://attainment.dypiu.ac.in",
                        "FLYWAY_ENABLED=false",
                        "HIBERNATE_DDL_AUTO=create-drop"
                )
                .run()) {
            assertNotNull(ctx);
            assertTrue(ctx.isRunning());
            assertEquals("https://attainment.dypiu.ac.in", ctx.getEnvironment().getProperty("app.cors.allowed-origins"));
            assertEquals("true", ctx.getEnvironment().getProperty("spring.flyway.clean-disabled"));
            assertEquals("true", ctx.getEnvironment().getProperty("spring.flyway.validate-on-migrate"));
            assertEquals("INFO", ctx.getEnvironment().getProperty("logging.level.com.dypiu.nba"));
        }
    }
}
