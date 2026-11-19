package com.freightfox.github.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchRequest(
        @NotBlank(message = "query is required")
        @Size(max = 200, message = "query must be at most 200 characters")
        String query,
        @Size(max = 64, message = "language must be at most 64 characters")
        String language,
        String sort
) {}
