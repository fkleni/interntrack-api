# InternTrack API

A RESTful API built with Spring Boot to track internship applications. It allows users to easily keep logs of the companies they applied to, their application status, and interview notes.

## Tech Stack

* **Java 17**
* **Spring Boot 3**
* **Spring Data JPA**
* **PostgreSQL**
* **Lombok**

## How to Run (Local Development)

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
