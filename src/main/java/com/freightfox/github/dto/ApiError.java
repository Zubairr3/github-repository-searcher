package com.freightfox.github.dto;

import java.time.Instant;

public record ApiError(String status, String message, Instant timestamp, String path) {}
