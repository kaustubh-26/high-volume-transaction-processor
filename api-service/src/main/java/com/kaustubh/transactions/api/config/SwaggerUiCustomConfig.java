package com.kaustubh.transactions.api.config;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerUiCustomConfig {

    @Bean
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
        SwaggerUiConfigProperties props = new SwaggerUiConfigProperties();

        props.setUrlsPrimaryName("default");
        props.setConfigUrl("/v3/api-docs/swagger-config");

        return props;
    }
}