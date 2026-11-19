package com.freightfox.github.api;

import com.freightfox.github.client.GithubApiClient;
import com.freightfox.github.dto.GithubSearchPayload;
import com.freightfox.github.repository.GithubRepositoryRepository;
import com.freightfox.github.service.SortOption;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GithubIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired GithubRepositoryRepository repository;
    @MockitoBean GithubApiClient githubApi;

    @BeforeEach void clean() { repository.deleteAll(); }

    private GithubSearchPayload.Item item(long stars, long forks) {
        return new GithubSearchPayload.Item(123L, "spring-boot-example", "An example",
                new GithubSearchPayload.Owner("alice"), "Java", stars, forks,
                Instant.parse("2026-01-01T12:00:00Z"));
    }

    @Test void searchesAndSavesThenFiltersThenUpdatesWithoutDuplicates() throws Exception {
        when(githubApi.search(eq("spring boot"), eq("Java"), eq(SortOption.STARS)))
                .thenReturn(List.of(item(450, 120)), List.of(item(600, 140)));
        String request = """
                  {"query":"spring boot","language":"Java","sort":"stars"}
                  """;

        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories[0].stars").value(450));
        mvc.perform(get("/api/github/repositories").param("language", "java")
                .param("minStars", "400").param("sort", "stars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories[0].id").value(123));
        mvc.perform(get("/api/github/repositories").param("minStars", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories").isEmpty());

        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories[0].stars").value(600));
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(123L).orElseThrow().toDto().forks()).isEqualTo(140);
    }

    @Test void emptyGithubSearchReturnsEmptyAndSavesNothing() throws Exception {
        when(githubApi.search(eq("none"), isNull(), eq(SortOption.STARS))).thenReturn(List.of());
        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"query":"none"}
                  """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories").isEmpty());
        assertThat(repository.count()).isZero();
    }

    @Test void appliesMultipleFiltersAndSortsDifferentRepositories() throws Exception {
        var newer = new GithubSearchPayload.Item(201L, "alpha", "first",
                new GithubSearchPayload.Owner("alice"), "Java", 200L, 10L,
                Instant.parse("2026-05-01T00:00:00Z"));
        var older = new GithubSearchPayload.Item(202L, "beta", "second",
                new GithubSearchPayload.Owner("bob"), "Python", 150L, 100L,
                Instant.parse("2026-01-01T00:00:00Z"));
        var writer = new com.freightfox.github.service.RepositoryPersistenceService(repository);
        writer.upsert(List.of(newer, older));
        mvc.perform(get("/api/github/repositories").param("sort", "forks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories[0].name").value("beta"));
        mvc.perform(get("/api/github/repositories").param("sort", "updated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories[0].name").value("alpha"));
        mvc.perform(get("/api/github/repositories").param("language", "jAvA")
                .param("minStars", "180"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories.length()").value(1))
                .andExpect(jsonPath("$.repositories[0].name").value("alpha"));
    }
}
