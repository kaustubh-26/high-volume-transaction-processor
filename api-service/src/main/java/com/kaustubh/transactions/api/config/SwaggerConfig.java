package com.kaustubh.transactions.api.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customConfig() {
        return new OpenAPI().info(
                new Info().title("High Volume Transaction Processor APIs")
                        .description("High Volume Transaction Processor APIs"))
                        .servers(Arrays.asList(new Server().url("http://localhost:8080").description("api-service")));
    }

}
