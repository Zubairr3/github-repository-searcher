package com.freightfox.github.service;

import com.freightfox.github.client.GithubApiClient;
import com.freightfox.github.dto.SearchRequest;
import com.freightfox.github.dto.SearchResponse;
import org.springframework.stereotype.Service;

@Service
public class GithubSearchService {
    private final GithubApiClient githubApiClient;
    private final RepositoryPersistenceService persistence;

    public GithubSearchService(GithubApiClient githubApiClient,
                               RepositoryPersistenceService persistence) {
        this.githubApiClient = githubApiClient;
        this.persistence = persistence;
    }

    public SearchResponse search(SearchRequest request) {
        SortOption sort = SortOption.parse(request.sort());
        var items = githubApiClient.search(request.query(), request.language(), sort);
        var saved = persistence.upsert(items);
        return new SearchResponse("Repositories fetched and saved successfully", saved);
    }
}
