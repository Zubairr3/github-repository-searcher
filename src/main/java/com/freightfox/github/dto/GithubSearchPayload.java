package com.freightfox.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubSearchPayload(List<Item> items) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Long id,
            String name,
            String description,
            Owner owner,
            String language,
            @JsonProperty("stargazers_count") Long stars,
            @JsonProperty("forks_count") Long forks,
            @JsonProperty("updated_at") Instant lastUpdated
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(String login) {}
}
