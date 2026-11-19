package com.freightfox.github.dto;

import java.util.List;

public record StoredRepositoriesResponse(List<RepositoryDto> repositories) {}
