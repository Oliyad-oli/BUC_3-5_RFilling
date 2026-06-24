package com.itas.taxfiling.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permits the bs-shared-ui apps (portal + back-office Next.js dev servers) to
 * call the service directly during local development. In a deployed
 * environment the API gateway terminates CORS — keep
 * {@code itas.cors.allowed-origins} empty there.
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Value("${itas.cors.allowed-origins:http://localhost:3000,http://localhost:3001,http://localhost:3002}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return;
        }
        registry.addMapping("/**")
            .allowedOrigins(allowedOrigins.split(","))
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("X-Correlation-Id")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
