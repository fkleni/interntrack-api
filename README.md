# InternTrack API

A RESTful API built with Spring Boot to track internship applications. It allows users to easily keep logs of the companies they applied to, their application status, and interview notes.

## Live Demo

The API is deployed and publicly accessible:

- **Swagger UI:** https://interntrack-api-2v39.onrender.com/swagger-ui/index.html

> **Note:** The free Render instance spins down after 15 minutes of inactivity. The first request after idling may take 30–60 seconds to respond while it wakes up.

## Tech Stack

* **Java 17**
* **Spring Boot 3**
* **Spring Data JPA**
* **Spring Security + JWT**
* **PostgreSQL**
* **Redis** (caching, via Spring Cache abstraction — Simple Cache locally, Redis in Codespaces)
* **Spring Mail + Spring Scheduler** (automated interview reminder emails)
* **Docker** (containerized deployment on Render)
* **Swagger / OpenAPI**
* **Lombok**

## Deployment Architecture

The application is deployed as a Docker container on **Render**, connected to a **Neon** PostgreSQL database (both free tier). Every push to the `main` branch automatically triggers a redeploy via Render's built-in CI/CD.

## How to Run

### Option 1: Local Development

1. Ensure PostgreSQL is running on your local machine.
2. Create a database and update the credentials in `src/main/resources/application.properties`:

```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/interntrack
   spring.datasource.username=your_username
   spring.datasource.password=your_password
```

3. Set the following environment variables for email notifications (see [Interview Reminders](#interview-reminders)):
   - `MAIL_USERNAME` — sending Gmail address
   - `MAIL_PASSWORD` — Gmail App Password (not your regular Gmail password)
4. Run the application using Maven:

```bash
   mvn spring-boot:run
```

5. The API will be available at `http://localhost:8080`.
6. Swagger UI: `http://localhost:8080/swagger-ui.html`

### Option 2: GitHub Codespaces

1. Go to the repository on GitHub.
2. Add `MAIL_USERNAME` and `MAIL_PASSWORD` as [Codespaces repository secrets](https://docs.github.com/en/codespaces/managing-your-codespaces/managing-secrets-for-your-codespaces) so they're injected automatically.
3. Click **Code > Codespaces > Create codespace on main**.
4. The container automatically installs Java 17, PostgreSQL, and Redis.
5. Run the application with the `codespaces` profile to enable Redis-backed caching:

```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=codespaces
```

6. Open the forwarded port 8080 URL and append `/swagger-ui.html`.

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Registers a new user. |
| `POST` | `/api/auth/login` | Authenticates a user and returns a JWT token. |
| `DELETE` | `/api/auth/delete-account` | Deletes the authenticated user's account and all their applications. Requires password confirmation. *(requires token)* |
| `POST` | `/api/applications` | Creates a new internship application, owned by the authenticated user. *(requires token)* |
| `GET` | `/api/applications` | Retrieves all applications belonging to the authenticated user. *(requires token)* |
| `GET` | `/api/applications/{id}` | Retrieves a specific application by ID, if it belongs to the authenticated user. *(requires token)* |
| `PUT` | `/api/applications/{id}` | Updates an existing application, if it belongs to the authenticated user. *(requires token)* |
| `DELETE` | `/api/applications/{id}` | Deletes an application, if it belongs to the authenticated user. *(requires token)* |
| `GET` | `/api/applications/dashboard` | Returns total application count and a status breakdown, scoped to the authenticated user. *(requires token)* |

### Example Request Body (`POST` / `PUT` for `/api/applications`)

```json
{
  "companyName": "Google",
  "position": "Backend Developer Intern",
  "status": "Applied",
  "appliedDate": "2026-07-17",
  "interviewDate": "2026-07-25",
  "notes": "Referral used."
}
```

`interviewDate` is optional — leave it out (or set it to `null`) until an interview is actually scheduled.

### Example Response

```json
{
  "id": 1,
  "companyName": "Google",
  "position": "Backend Developer Intern",
  "status": "Applied",
  "appliedDate": "2026-07-17",
  "interviewDate": "2026-07-25",
  "notes": "Referral used."
}
```

## Dashboard & Caching

The `/api/applications/dashboard` endpoint returns the total number of applications and a dynamic breakdown by status (grouped by whatever status values actually exist in the database, rather than a fixed set), scoped to the authenticated user.

```json
{
  "total": 2,
  "statusCounts": {
    "Applied": 2
  }
}
```

Results are cached to avoid recomputing the breakdown on every request. The cache is invalidated automatically whenever an application is created, updated, or deleted. The application uses Spring's Cache abstraction, so the same `@Cacheable`/`@CacheEvict` code works with two different backends depending on the environment:

- **Local development:** in-memory Simple Cache (no external dependency required).
- **GitHub Codespaces:** Redis, running in the container.

Since dashboard data is scoped per user, the cache key is tied to the authenticated username — each user gets their own cached breakdown, and one user's dashboard is never served to another.

## Interview Reminders

A scheduled job runs every 5 minutes and checks for applications with an `interviewDate` set to today or tomorrow. For each match, it sends a reminder email — via Gmail SMTP using `JavaMailSender` — to the application owner's registered email address.

To avoid sending duplicate reminders, each application tracks a `lastReminderSentDate`. This is only updated after a reminder email is confirmed sent successfully; if the email fails (e.g. an SMTP error), the send is retried on the next scheduled run instead of being silently skipped.

Mail credentials are never hardcoded or committed to the repository. They're read from environment variables:

```properties
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

`MAIL_PASSWORD` should be a [Gmail App Password](https://myaccount.google.com/apppasswords), generated separately from your main account password, so it can be revoked independently if ever exposed.

## API Documentation (Swagger)

Interactive API documentation is available via Swagger UI at `/swagger-ui.html`. All protected endpoints can be tested directly from the browser after authorizing with a JWT token (obtained from `/api/auth/login`) via the **Authorize** button.

### Validation

Requests with missing or invalid fields (`companyName`, `position`, `status`, `appliedDate`) are handled by a centralized `GlobalExceptionHandler`. It intercepts validation failures and returns a structured `400 Bad Request` response detailing each failing field, instead of a generic server error.

#### Example Error Response

```json
{
  "timestamp": "2026-07-28T14:30:15.12345",
  "status": 400,
  "errors": {
    "companyName": "Company name cannot be blank",
    "appliedDate": "Applied date cannot be in the future"
  }
}
```

Requests to non-existent resources (e.g., `GET /api/applications/999`) — or to a resource that belongs to a different user — return a `404 Not Found` with a similarly structured error message.

## Authentication

The API uses JWT (JSON Web Token) based authentication. All endpoints under `/api/applications` require a valid token; only registration and login are publicly accessible.

### Register

```
POST /api/auth/register
```

```json
{
  "username": "your_username",
  "email": "your_email@example.com",
  "password": "your_password"
}
```

### Login

```
POST /api/auth/login
```

```json
{
  "username": "your_username",
  "email": "your_email@example.com",
  "password": "your_password"
}
```

Returns a JWT token on success:

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9..."
}
```

### Using the Token

Include the token in the `Authorization` header for all protected endpoints:

```http
Authorization: Bearer <your_token>
```

Requests without a valid token return a `401 Unauthorized` response:

```json
{
  "timestamp": "2026-08-07T11:27:30.787292700",
  "status": 401,
  "message": "Authentication required. Please provide a valid token."
}
```

### Deleting Your Account

```
DELETE /api/auth/delete-account
Authorization: Bearer <your_token>
```

```json
{
  "password": "your_password"
}
```

Permanently deletes the authenticated user's account along with all of their application records. The account to delete is always derived from the JWT token, never from the request body — so a valid token for one user can never be used to delete another user's account. Requires the correct current password in the request body; an incorrect password returns `401 Unauthorized`.

## Known Limitations

- The free Render/Neon hosting tier has its own constraints (cold starts, SMTP port restrictions in production) — see the note at the top of this README.
