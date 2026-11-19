package com.freightfox.github.api;

import com.freightfox.github.dto.ApiError;
import com.freightfox.github.dto.SearchRequest;
import com.freightfox.github.dto.SearchResponse;
import com.freightfox.github.dto.StoredRepositoriesResponse;
import com.freightfox.github.service.GithubSearchService;
import com.freightfox.github.service.StoredRepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/github")
@Tag(name = "GitHub Repositories", description = "Search and retrieve public GitHub repositories")
public class GithubController {
    private final GithubSearchService searchService;
    private final StoredRepositoryService storedService;

    public GithubController(GithubSearchService searchService,
                            StoredRepositoryService storedService) {
        this.searchService = searchService;
        this.storedService = storedService;
    }

    @PostMapping("/search")
    @Operation(summary = "Search GitHub repositories and store the results")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Repositories searched and saved"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "429", description = "GitHub search rate limit reached",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "502", description = "GitHub returned an invalid/error response",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "504", description = "GitHub request timed out",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }

    @GetMapping("/repositories")
    @Operation(summary = "Filter and sort repositories already saved in PostgreSQL")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stored repositories returned"),
        @ApiResponse(responseCode = "400", description = "Invalid filter or sort parameter",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<StoredRepositoriesResponse> repositories(
            @RequestParam(required = false) @Size(max = 64) String language,
            @RequestParam(required = false) @Min(0) Long minStars,
            @RequestParam(defaultValue = "stars") String sort) {
        return ResponseEntity.ok(storedService.find(language, minStars, sort));
    }
}
