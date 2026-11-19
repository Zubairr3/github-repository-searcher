package com.freightfox.github.service;

import com.freightfox.github.api.BadRequestException;
import java.util.Locale;

public enum SortOption {
    STARS("stars", "stars"),
    FORKS("forks", "forks"),
    UPDATED("updated", "lastUpdated");

    private final String githubValue;
    private final String entityField;

    SortOption(String githubValue, String entityField) {
        this.githubValue = githubValue;
        this.entityField = entityField;
    }

    public String githubValue() { return githubValue; }
    public String entityField() { return entityField; }

    public static SortOption parse(String raw) {
        if (raw == null || raw.isBlank()) return STARS;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "stars" -> STARS;
            case "forks" -> FORKS;
            case "updated" -> UPDATED;
            default -> throw new BadRequestException("sort must be stars, forks, or updated");
        };
    }
}
