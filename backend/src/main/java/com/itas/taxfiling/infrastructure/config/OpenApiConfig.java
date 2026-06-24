package com.itas.taxfiling.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tax Filing & Processing API")
                        .version("1.0.0")
                        .description("BUC-003 (Return Filing) & BUC-005 (Return Processing) API")
                        .contact(new Contact()
                                .name("ITAS Team")
                                .email("support@itas.gov.et"))
                );
    }
}
