# FreightFox - GitHub Repository Searcher

**Java 21 + Spring Boot 3.5 + PostgreSQL + GitHub REST API + Swagger + JUnit + Docker**

This is an API-only implementation of the supplied FreightFox **Backend Assignment - GitHub Repository Searcher**. No UI, LinkedIn account, PhantomBuster account, or GitHub login is required for public GitHub repository searches. A `GITHUB_TOKEN` is **optional** and may provide a higher GitHub API search quota.

## 1. Assignment coverage

| Requirement in PDF | Implementation |
| --- | --- |
| POST `/api/github/search` | Accepts `query`, optional `language`, optional `sort`; calls real public `GET https://api.github.com/search/repositories` |
| Search by repository name | GitHub search query with `in:name`; exact or partial name keywords |
| Sort by stars, forks, updated | `stars` (default), `forks`, `updated`; GitHub sort parameter |
| Persist to PostgreSQL | `GithubRepository` JPA entity with GitHub ID as primary key |
| Update existing repository | `RepositoryPersistenceService.upsert`: reads existing IDs then updates/reuses those entities |
| GET `/api/github/repositories` | Optional `language`, `minStars` and `sort` |
| Empty results | HTTP 200, empty `repositories: []`; database unchanged |
| Invalid request | HTTP 400 with field error JSON |
| GitHub errors | HTTP 429 for rate limiting, 400 for rejected search, 502 for other upstream/invalid response, 504 timeout |
| Testing | Mocked GitHub HTTP responses + controller/service tests + H2 persistence integration test |
| API docs | Swagger `/swagger-ui.html`, OpenAPI `/v3/api-docs` |

**Scope:** One GitHub search request fetches the first **30** matching results, not every result in GitHub. The app does not invent repositories or scrape GitHub web pages. GitHub's search API may report `incomplete_results` for large searches; this app stores the returned items. Database operations use Hibernate `ddl-auto=update` for assignment setup; for real production deployment, use versioned migrations.

## 2. Architecture

```text
POST /api/github/search
   -> GithubController (HTTP + request validation)
   -> GithubSearchService (coordinates work)
   -> GithubApiClient (real GitHub API; no personal account required)
   -> RepositoryPersistenceService (transactional upsert)
   -> GithubRepositoryRepository (Spring Data JPA)
   -> PostgreSQL

GET /api/github/repositories
   -> GithubController
   -> StoredRepositoryService (optional filters + sort)
   -> GithubRepositoryRepository -> PostgreSQL
```

Classes are small and focused: see [docs/LEARNING_GUIDE.md](docs/LEARNING_GUIDE.md) for every file and what to say during review.

**Start here:** [docs/START_TO_SUBMISSION.md](docs/START_TO_SUBMISSION.md) provides a single numbered checklist from first terminal command to Loom and submission.

## 3. Windows + VS Code: run with Docker Desktop

**Prerequisite:** Docker Desktop running, Docker Compose available, ports **8081** and **5433** free. This project intentionally uses different host ports from the earlier Alumni project.

Open the folder containing `pom.xml`, `Dockerfile`, and `compose.yaml` in VS Code. In a PowerShell terminal:

```powershell
Copy-Item .env.example .env
# Leave GITHUB_TOKEN blank. Public GitHub search works without it.
docker compose up --build -d
docker compose ps
docker compose logs --tail=80 api
```

Open:

- Health: http://localhost:8081/actuator/health (expected `{"status":"UP"}`)
- Swagger: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs

If you are running the original Alumni containers, you can keep them running because this Compose project uses ports 8081/5433. Stop this project with `docker compose down` (do **not** add `-v` unless you want to delete its database volume).

## 4. Try the two APIs

### Search and save real public repositories

`POST http://localhost:8081/api/github/search` with `Content-Type: application/json`:

```json
{"query":"spring boot","language":"Java","sort":"stars"}
```

Response structure (illustrative; actual GitHub data changes):

```json
{
  "message": "Repositories fetched and saved successfully",
  "repositories": [
    {"id":123456,"name":"spring-boot-example","description":"Example only",
     "owner":"developer","language":"Java","stars":450,"forks":120,
     "lastUpdated":"2026-01-01T12:00:00Z"}
  ]
}
```

The example values above are **not live results**. Actual results depend on GitHub.

### Read already saved data (no outbound GitHub call)

`GET http://localhost:8081/api/github/repositories?language=Java&minStars=100&sort=stars`

Example structure: `{"repositories":[...]}`. `sort=stars`, `sort=forks`, `sort=updated` are descending. Language is case-insensitive; minStars is inclusive. Without filters, GET returns everything saved locally. GET does **not** search GitHub.

### Invalid requests

- Empty `query`, too-long strings, unknown `sort`, negative `minStars` -> HTTP 400.
- GitHub rate limit (HTTP 429 or 403 with limit headers) -> HTTP 429 with explanatory message; wait before retrying. Other GitHub HTTP 403 errors -> HTTP 502.
- GitHub returned malformed/incomplete data -> HTTP 502.
- GitHub network timeout -> HTTP 504.
- Empty GitHub results -> HTTP 200 and an empty list.

## 5. Automated tests

In the project folder, with Docker Desktop running:

```powershell
docker build --target test -t github-searcher-tests .
```

The test stage executes `mvn test` inside the image build, and should end with `BUILD SUCCESS`; it does not contact GitHub or require PostgreSQL. Alternatively if Java 21 + Maven are installed locally: `mvn test`.

Test classes cover: sort parsing, mapping the public GitHub API response, rate limits, invalid JSON, blank input, HTTP endpoints, persistence, filters, duplicate updates, and empty results. **Do not claim tests passed until you actually run them.**

## 6. Verify PostgreSQL persistence and upsert

1. Execute POST `/api/github/search` in Swagger with a Java repository query.
2. Execute GET `/api/github/repositories` and observe saved results.
3. Repeat POST with the same query. GitHub IDs must remain unique, rather than creating duplicate DB rows.
4. Run `docker compose restart api`, then GET again. Saved records must still be present.
5. Optional: `docker compose exec db psql -U github_user -d github_repositories -c "select repository_id,repo_name,stars_count from github_repositories order by stars_count desc limit 10;"`

## 7. Deployment and Loom

- Read [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) and [docs/LOOM_SCRIPT.md](docs/LOOM_SCRIPT.md).
- Host application and a persistent PostgreSQL database; set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and optional `GITHUB_TOKEN` as cloud environment variables.
- Confirm public `/actuator/health`, `/swagger-ui.html`, POST and GET *actually work* before submitting.
- Never commit `.env` or expose secrets in the video.

## 8. Official sources only

- FreightFox PDF: `Backend Assignment - GitHub Repository Searcher.pdf` (provided separately in the user's assignment archive)
- GitHub Search REST API: https://docs.github.com/en/rest/search/search#search-repositories
- GitHub search syntax: https://docs.github.com/en/search-github/searching-on-github/searching-for-repositories
- GitHub rate limits: https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api
- GitHub REST API getting started/headers: https://docs.github.com/en/rest/using-the-rest-api/getting-started-with-the-rest-api
- Oracle Java 21 API: https://docs.oracle.com/en/java/javase/21/docs/api/
- Spring Boot 3.5 reference: https://docs.spring.io/spring-boot/3.5/reference/
- Spring Framework REST clients: https://docs.spring.io/spring-framework/reference/6.2/integration/rest-clients.html
- Spring Data JPA Specifications: https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/specifications.html
- Spring Boot testing: https://docs.spring.io/spring-boot/3.5/reference/testing/spring-boot-applications.html
- PostgreSQL official manual: https://www.postgresql.org/docs/current/
- Docker Compose official reference: https://docs.docker.com/compose/
- Springdoc OpenAPI official project: https://springdoc.org/

## Limitations and production improvements

- No pagination across multiple GitHub result pages (first 30 only).
- Search API endpoint is public by default; rate limiting/authentication would be needed for broader public exposure.
- GitHub search may return partial results under load; this implementation persists those returned.
- GitHub's statistics can change; repeat POST to refresh.
- `ddl-auto=update` is acceptable for assignment setup but is not a substitute for Flyway/Liquibase schema migrations on mature production systems.
