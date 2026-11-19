package com.freightfox.github.api;

import com.freightfox.github.dto.SearchResponse;
import com.freightfox.github.dto.StoredRepositoriesResponse;
import com.freightfox.github.service.GithubSearchService;
import com.freightfox.github.service.StoredRepositoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GithubController.class)
class GithubControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean GithubSearchService searchService;
    @MockitoBean StoredRepositoryService storedService;

    @Test void postSearchReturnsExpectedShape() throws Exception {
        when(searchService.search(any())).thenReturn(new SearchResponse("Repositories fetched and saved successfully", List.of()));
        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"query":"spring boot","language":"Java","sort":"stars"}
                  """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repositories fetched and saved successfully"))
                .andExpect(jsonPath("$.repositories").isArray());
    }

    @Test void blankQueryIsRejectedBeforeService() throws Exception {
        mvc.perform(post("/api/github/search").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"query":"   ","sort":"stars"}
                  """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
        verifyNoInteractions(searchService);
    }

    @Test void getStoredRepositoriesReturnsExpectedShape() throws Exception {
        when(storedService.find("Java", 100L, "forks"))
                .thenReturn(new StoredRepositoriesResponse(List.of()));
        mvc.perform(get("/api/github/repositories")
                .param("language", "Java").param("minStars", "100").param("sort", "forks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories").isArray());
    }

    @Test void negativeMinStarsIsRejected() throws Exception {
        mvc.perform(get("/api/github/repositories").param("minStars", "-1"))
                .andExpect(status().isBadRequest());
    }
}
