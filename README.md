# InternTrack API

A RESTful API built with Spring Boot to track internship applications. It allows users to easily keep logs of the companies they applied to, their application status, and interview notes.

## Tech Stack

* **Java 17**
* **Spring Boot 3**
* **Spring Data JPA**
* **PostgreSQL**
* **Lombok**

## How to Run (Local Development)

### Option 1: Local Development
1. Ensure PostgreSQL is running on your local machine.
2. Create a database and update the credentials in `src/main/resources/application.properties`:
```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/interntrack
   spring.datasource.username=your_username
   spring.datasource.password=your_password
```
3. Run the application using Maven:
```bash
   mvn spring-boot:run
```
4. The API will be available at `http://localhost:8080`.

### Option 2: GitHub Codespaces
1. Go to the repository on GitHub.
2. Click **Code > Codespaces > Create codespace on main**.
3. The container automatically installs Java 17 and PostgreSQL.
4. Run the application from the Codespaces terminal:
   ```bash
   ./mvnw spring-boot:run

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/applications` | Creates a new internship application. |
| `GET` | `/api/applications` | Retrieves a list of all applications. |
| `GET` | `/api/applications/{id}` | Retrieves details of a specific application by its ID. |
| `PUT` | `/api/applications/{id}` | Updates an existing application. |
| `DELETE` | `/api/applications/{id}` | Deletes an application from the system. |

### Example Request Body (`POST` / `PUT`)

```json
{
  "companyName": "Google",
  "position": "Backend Developer Intern",
  "status": "Applied",
  "appliedDate": "2026-07-17",
  "notes": "Referral used."
}
```

### Example Response

```json
{
  "id": 1,
  "companyName": "Google",
  "position": "Backend Developer Intern",
  "status": "Applied",
  "appliedDate": "2026-07-17",
  "notes": "Referral used."
}
```

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

Requests to non-existent resources (e.g., `GET /api/applications/999`) return a `404 Not Found` with a similarly structured error message.
