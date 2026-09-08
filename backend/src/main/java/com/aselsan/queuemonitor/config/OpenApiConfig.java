package com.aselsan.queuemonitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI queueMonitorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Queue Monitor API")
                        .description(
                                "Bounded queue üzerindeki sender ve receiver "
                                        + "worker simülasyonunu yönetir ve izler."
                        )
                        .version("v1"));
    }
}
