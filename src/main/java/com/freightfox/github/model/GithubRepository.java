package com.freightfox.github.model;

import com.freightfox.github.dto.GithubSearchPayload;
import com.freightfox.github.dto.RepositoryDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "github_repositories")
public class GithubRepository {
    @Id
    @Column(name = "repository_id")
    private Long id; // GitHub's own globally unique repository ID, not an auto-generated local ID.

    @Column(name = "repo_name", nullable = false, length = 255)
    private String name;

    @Column(name = "repo_description", columnDefinition = "text")
    private String description;

    @Column(name = "owner_login", nullable = false, length = 255)
    private String owner;

    @Column(name = "primary_language", length = 100)
    private String language;

    @Column(name = "stars_count", nullable = false)
    private long stars;

    @Column(name = "forks_count", nullable = false)
    private long forks;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdated;

    protected GithubRepository() { } // JPA requires a no-argument constructor.

    public GithubRepository(Long id) { this.id = id; }

    public void updateFrom(GithubSearchPayload.Item item) {
        name = item.name();
        description = item.description();
        owner = item.owner().login();
        language = item.language();
        stars = item.stars();
        forks = item.forks();
        lastUpdated = item.lastUpdated();
    }

    public RepositoryDto toDto() {
        return new RepositoryDto(id, name, description, owner, language, stars, forks, lastUpdated);
    }
}
