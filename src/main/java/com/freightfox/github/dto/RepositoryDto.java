package com.freightfox.github.dto;

import java.time.Instant;

public record RepositoryDto(
        long id,
        String name,
        String description,
        String owner,
        String language,
        long stars,
        long forks,
        Instant lastUpdated
) {}
