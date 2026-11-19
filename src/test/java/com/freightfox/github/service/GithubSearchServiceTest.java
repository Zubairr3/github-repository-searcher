package com.freightfox.github.service;

import com.freightfox.github.client.GithubApiClient;
import com.freightfox.github.dto.GithubSearchPayload;
import com.freightfox.github.dto.RepositoryDto;
import com.freightfox.github.dto.SearchRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubSearchServiceTest {
    @Mock GithubApiClient api;
    @Mock RepositoryPersistenceService persistence;
    @InjectMocks GithubSearchService service;

    @Test void searchesThenPersists() {
        var item = new GithubSearchPayload.Item(7L, "repo", "desc",
                new GithubSearchPayload.Owner("alice"), "Java", 5L, 2L, Instant.parse("2026-01-01T00:00:00Z"));
        var dto = new RepositoryDto(7, "repo", "desc", "alice", "Java", 5, 2,
                Instant.parse("2026-01-01T00:00:00Z"));
        when(api.search("spring", "Java", SortOption.STARS)).thenReturn(List.of(item));
        when(persistence.upsert(List.of(item))).thenReturn(List.of(dto));
        var result = service.search(new SearchRequest("spring", "Java", "stars"));
        assertThat(result.repositories()).containsExactly(dto);
        verify(persistence).upsert(List.of(item));
    }

    @Test void emptySearchDoesNotInventRepositories() {
        when(api.search("nothing", null, SortOption.FORKS)).thenReturn(List.of());
        when(persistence.upsert(List.of())).thenReturn(List.of());
        assertThat(service.search(new SearchRequest("nothing", null, "forks"))
                .repositories()).isEmpty();
    }
}
