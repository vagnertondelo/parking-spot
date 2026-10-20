package com.api.parkingcontrol.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS (necessário apenas para Flutter Web; Android/iOS não usam CORS).
 * Por padrão libera qualquer origem ("*"). Para restringir, defina a variável de ambiente
 * CORS_ALLOWED_ORIGINS com as origens separadas por vírgula, ex.: https://meuapp.com,http://localhost:5000
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .maxAge(3600);
    }

}
