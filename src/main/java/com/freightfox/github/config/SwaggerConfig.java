package com.freightfox.github.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    OpenAPI githubRepositoryOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FreightFox GitHub Repository Searcher")
                .version("1.0.0")
                .description("Search public GitHub repositories, upsert to PostgreSQL, and query stored results."));
    }
}
