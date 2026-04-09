package com.cyclerouteplanner.backend.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Cycle Route Planner Backend API")
                .version("v1")
                .description("Backend API for cycling routing and geodata integrations"));
    }
}
