GitHub Repository Searcher
A Java Spring Boot REST API for searching public GitHub repositories, storing repository metadata in PostgreSQL, and querying saved repositories using filters and sorting.
Tech stack
- Java 21 and Spring Boot 3.5
- Spring Web (RestClient) for GitHub API integration
- Spring Data JPA / Hibernate and PostgreSQL for persistence
- Jakarta Bean Validation for request validation
- springdoc-openapi / Swagger UI for API documentation
- JUnit 5, Mockito, and H2 for automated tests
- Docker and Docker Compose for local setup
Features
- Search public repositories by name keywords, with an optional programming-language filter.
- Sort GitHub search results by stars, forks, or last update.
- Save repository details, including owner, language, description, stars, forks, and update timestamp.
- Update an existing record when a repository with the same GitHub ID is found.
- Filter stored records by language and minimum star count, with configurable sorting.
- Return structured HTTP errors for invalid requests, GitHub API failures, rate limits, and timeouts.
API endpoints
Method	Endpoint	Description
POST	/api/github/search	Search the public GitHub API and save returned repositories.
GET	/api/github/repositories	Retrieve saved repositories, optionally filtered and sorted.


Search and save
POST /api/github/search
{
  "query": "spring boot",
  "language": "Java",
  "sort": "stars"
}
- query is required. Search terms are matched against repository names.
- language is optional.
- sort accepts stars (default), forks, or updated.
- Results are ordered descending; up to 30 repositories are retrieved from the first GitHub search page per request.
A successful request returns HTTP 200 with a message and a repositories array containing repository metadata.
Query saved repositories
GET /api/github/repositories?language=Java&minStars=10000&sort=stars
- language: optional, case-insensitive match.
- minStars: optional, inclusive minimum star count.
- sort: stars, forks, or updated, descending.
This endpoint reads from PostgreSQL; it does not make a new GitHub API request.
Architecture
POST /api/github/search
  GithubController
    -> GithubSearchService
       -> GithubApiClient -> GitHub REST API
       -> RepositoryPersistenceService
          -> GithubRepositoryRepository -> PostgreSQL

GET /api/github/repositories
  GithubController
    -> StoredRepositoryService
       -> GithubRepositoryRepository -> PostgreSQL
Controllers handle HTTP requests and validation. Services coordinate search, persistence, and filtering. GithubApiClient isolates external API communication. The repository interface handles database access through Spring Data JPA.

GitHub repository IDs are used as primary keys. The persistence service loads existing entities and updates their fields before saving, avoiding duplicate rows for repeated searches.
Run locally
Prerequisite: Docker Desktop with Docker Compose.

From the project root in PowerShell:
Copy-Item .env.example .env
# Optionally change DB_PASSWORD in .env
docker compose up --build -d
docker compose ps
Local services:
Service	URL / Port
API	http://localhost:8081
Health	http://localhost:8081/actuator/health
Swagger UI	http://localhost:8081/swagger-ui.html
OpenAPI specification	http://localhost:8081/v3/api-docs
PostgreSQL	localhost:5433


To view application logs:
docker compose logs --tail=100 api
To stop the containers without deleting the database volume:
docker compose down
Configuration
Variable	Purpose
DB_URL	PostgreSQL JDBC URL; provided by Docker Compose for local use.
DB_USERNAME	Database username.
DB_PASSWORD	Database password.
GITHUB_TOKEN	Optional token for GitHub API requests. Public searches can be made without one, subject to rate limits.
PORT	Application HTTP port inside the container (default 8080).


Do not commit .env or real credentials to version control.
Tests
Run the test stage with Docker:
docker build --target test -t github-repository-searcher-tests .
Alternatively, with Java 21 and Maven installed:
mvn test
The test suite covers API controllers, request validation, GitHub response mapping and error handling, service behavior, sorting, and persistence using an H2 test database. 22 tests passed in the local Maven run.
Error handling
HTTP status	Situation
400	Invalid request fields, unsupported sort option, or GitHub rejecting a search query.
429	GitHub search rate limit reached.
502	GitHub API error or invalid upstream response.
504	Timeout while contacting GitHub.


Limitations
- Each search retrieves only the first page of GitHub results (up to 30 repositories); pagination is not implemented.
- GitHub statistics can change. Repeating a search refreshes metadata for repositories returned again.
- The search endpoint has no application-level authentication or rate limiting; these should be added before broader public use.
- Hibernate schema auto-update is used for simple setup. A production system should use managed, versioned database migrations.
