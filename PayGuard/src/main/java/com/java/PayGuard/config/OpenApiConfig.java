package com.java.PayGuard.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "PayGuard API",
        version = "1.0.0",
        description = "PayGuard Fintech Backend — Real-time P2P wallet transfers, " +
                      "idempotency protection, automated fraud detection, RBAC analyst & admin portals, " +
                      "and complete Personal Expense Tracker with Activity-Linked Invoices.",
        contact = @Contact(name = "PayGuard Team")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development"),
        @Server(url = "http://localhost",       description = "Docker Compose (via Nginx proxy)")
    }
)
@SecurityScheme(
    name = "cookieAuth",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.COOKIE,
    paramName = "jwt",
    description = "HTTP-Only JWT cookie set on login via POST /api/v1/auth/login"
)
public class OpenApiConfig {
    // Configuration is annotation-driven — no beans required
}
