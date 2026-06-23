package kz.applications.daramaven.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        String securitySchemaName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Daramaven API")
                        .description("Spring Boot REST API with JWT, refresh tokens, sessions, admin tools and audit logs")
                        .version("1.0.0")
                )

                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemaName)
                )

                .components(new Components()
                        .addSecuritySchemes(
                                securitySchemaName,
                                new SecurityScheme()
                                        .name(securitySchemaName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}
