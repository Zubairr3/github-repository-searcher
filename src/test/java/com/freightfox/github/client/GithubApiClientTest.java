package com.freightfox.github.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.freightfox.github.api.GithubUpstreamException;
import com.freightfox.github.service.SortOption;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GithubApiClientTest {
    private GithubApiClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GithubApiClient(builder.build(),
                new ObjectMapper().registerModule(new JavaTimeModule()), 30);
    }

    @Test
    void mapsResultsAndBuildsNameAndLanguageSearch() {
        server.expect(request -> {
                    var uri = request.getURI();
                    assertThat(uri.getPath()).isEqualTo("/search/repositories");
                    var query = URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8);
                    assertThat(query).contains("in:name", "language:", "sort=stars", "per_page=30");
                })
                .andRespond(withSuccess("""
                    {"total_count":1,"items":[{"id":123,"name":"spring-boot-example",
                    "description":"Example repository","owner":{"login":"developer"},
                    "language":"Java","stargazers_count":450,"forks_count":120,
                    "updated_at":"2026-01-01T12:00:00Z"}]}
                    """, MediaType.APPLICATION_JSON));
        var results = client.search("spring boot", "Java", SortOption.STARS);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).owner().login()).isEqualTo("developer");
        assertThat(results.get(0).stars()).isEqualTo(450);
        server.verify();
    }

    @Test
    void emptySearchReturnsEmptyList() {
        server.expect(request ->
                        assertThat(request.getURI().getPath()).isEqualTo("/search/repositories"))
                .andRespond(withSuccess("""
                    {"total_count":0,"items":[]}
                    """, MediaType.APPLICATION_JSON));
        assertThat(client.search("nonexistent", null, SortOption.FORKS)).isEmpty();
        server.verify();
    }

    @Test
    void rateLimitBecomesHttp429() {
        server.expect(request -> { })
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> client.search("java", null, SortOption.STARS))
                .isInstanceOfSatisfying(GithubUpstreamException.class,
                        ex -> assertThat(ex.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        server.verify();
    }

    @Test
    void github403WithoutRateLimitHeaderIsUpstreamFailure() {
        server.expect(request -> { })
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> client.search("java", null, SortOption.STARS))
                .isInstanceOfSatisfying(GithubUpstreamException.class,
                        ex -> assertThat(ex.status()).isEqualTo(HttpStatus.BAD_GATEWAY));
        server.verify();
    }

    @Test
    void github403WithRateLimitHeaderBecomesHttp429() {
        server.expect(request -> { })
                .andRespond(withStatus(HttpStatus.FORBIDDEN).header("X-RateLimit-Remaining", "0"));
        assertThatThrownBy(() -> client.search("java", null, SortOption.STARS))
                .isInstanceOfSatisfying(GithubUpstreamException.class,
                        ex -> assertThat(ex.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        server.verify();
    }

    @Test
    void invalidSearchBecomesHttp400() {
        server.expect(request -> { })
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> client.search("java", null, SortOption.STARS))
                .isInstanceOfSatisfying(GithubUpstreamException.class,
                        ex -> assertThat(ex.status()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void unexpectedGitHubServerErrorBecomesHttp502() {
        server.expect(request -> { })
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.search("java", null, SortOption.STARS))
                .isInstanceOfSatisfying(GithubUpstreamException.class,
                        ex -> assertThat(ex.status()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void malformedJsonBecomesHttp502() {
        server.expect(request -> { })
                .andRespond(withSuccess("{broken", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.search("java", null, SortOption.UPDATED))
                .isInstanceOfSatisfying(GithubUpstreamException.class,
                        ex -> assertThat(ex.status()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void missingItemsIsRejected() {
        server.expect(request -> { })
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.search("java", null, SortOption.STARS))
                .isInstanceOfSatisfying(GithubUpstreamException.class,
                        ex -> assertThat(ex.status()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void rejectsRepositoryWithMissingOwner() {
        server.expect(request -> { })
                .andRespond(withSuccess("""
                    {"items":[{"id":42,"name":"repo","stargazers_count":10,
                    "forks_count":2,"updated_at":"2026-01-01T00:00:00Z"}]}
                    """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.search("repo", null, SortOption.STARS))
                .isInstanceOfSatisfying(GithubUpstreamException.class,
                        ex -> assertThat(ex.status()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
