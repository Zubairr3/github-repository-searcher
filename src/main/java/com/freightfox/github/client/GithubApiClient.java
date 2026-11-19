package com.freightfox.github.client;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freightfox.github.api.GithubUpstreamException;
import com.freightfox.github.dto.GithubSearchPayload;
import com.freightfox.github.service.SortOption;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GithubApiClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final int perPage;

    public GithubApiClient(
            @Qualifier("githubRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${github.per-page:30}") int perPage) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.perPage = perPage;
    }

    public List<GithubSearchPayload.Item> search(String keywords, String language, SortOption sort) {
        // 'in:name' focuses the free-text terms on repository names, as requested in the PDF.
        String searchQuery = keywords.trim() + " in:name";
        if (language != null && !language.isBlank()) {
            searchQuery += " language:\"" + language.trim().replace("\"", "") + "\"";
        }
        final String q = searchQuery;

        try {
            return restClient.get()
                    .uri(builder -> builder.path("/search/repositories")
                            .queryParam("q", q)
                            .queryParam("sort", sort.githubValue())
                            .queryParam("order", "desc")
                            .queryParam("per_page", perPage)
                            .build())
                    .exchange((request, response) -> {
                        int code = response.getStatusCode().value();
                        if (code == 429 || (code == 403 && isRateLimited(response))) {
                            throw new GithubUpstreamException(HttpStatus.TOO_MANY_REQUESTS,
                                    "GitHub search rate limit reached; retry later");
                        }
                        if (code == 403) {
                            throw new GithubUpstreamException(HttpStatus.BAD_GATEWAY,
                                    "GitHub denied the search request (HTTP 403)");
                        }
                        if (code == 422) {
                            throw new GithubUpstreamException(HttpStatus.BAD_REQUEST,
                                    "GitHub rejected the search query; simplify your keywords or language filter");
                        }
                        if (code != 200) {
                            throw new GithubUpstreamException(HttpStatus.BAD_GATEWAY,
                                    "GitHub API returned HTTP " + code);
                        }
                        try {
                            GithubSearchPayload payload = objectMapper.readValue(
                                    response.getBody(), GithubSearchPayload.class);
                            if (payload == null || payload.items() == null) {
                                throw invalidResponse();
                            }
                            for (GithubSearchPayload.Item item : payload.items()) {
                                if (item == null || item.id() == null || item.id() <= 0
                                        || item.name() == null || item.name().isBlank()
                                        || item.owner() == null || item.owner().login() == null
                                        || item.owner().login().isBlank()
                                        || item.stars() == null || item.stars() < 0
                                        || item.forks() == null || item.forks() < 0
                                        || item.lastUpdated() == null) {
                                    throw invalidResponse();
                                }
                            }
                            return payload.items();
                        } catch (IOException ex) {
                            throw invalidResponse();
                        }
                    });
        } catch (GithubUpstreamException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new GithubUpstreamException(HttpStatus.GATEWAY_TIMEOUT,
                    "Timed out while connecting to GitHub");
        } catch (RestClientException ex) {
            throw new GithubUpstreamException(HttpStatus.BAD_GATEWAY,
                    "Could not contact GitHub API");
        }
    }

    private boolean isRateLimited(org.springframework.http.client.ClientHttpResponse response) {
        return "0".equals(response.getHeaders().getFirst("X-RateLimit-Remaining"))
                || response.getHeaders().containsKey("Retry-After");
    }

    private GithubUpstreamException invalidResponse() {
        return new GithubUpstreamException(HttpStatus.BAD_GATEWAY,
                "GitHub API returned an invalid or incomplete response");
    }
}
