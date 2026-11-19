package com.freightfox.github.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubClientConfig {
    @Bean("githubRestClient")
    public RestClient githubRestClient(
            RestClient.Builder builder,
            @Value("${github.token:}") String token) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));

        RestClient.Builder configured = builder
                .baseUrl("https://api.github.com")
                .requestFactory(factory)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "freightfox-github-repository-searcher");
        if (token != null && !token.isBlank()) {
            configured.defaultHeader("Authorization", "Bearer " + token.trim());
        }
        return configured.build();
    }
}
