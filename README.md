# OnlineRecruitment

Java 17, Spring Boot and SQL Server recruitment application.

## Local configuration

Set the following environment variables before starting the application:

- `SPRING_DATASOURCE_URL`: JDBC URL for the existing SQL Server database (optional; defaults to localhost / OnlineRecruitment).
- `SPRING_DATASOURCE_USERNAME`: database username (optional; defaults to sa).
- `SPRING_DATASOURCE_PASSWORD`: database password.
- `SPRING_MAIL_USERNAME`: SMTP account used to send OTP email.
- `SPRING_MAIL_PASSWORD`: SMTP app password.

Do not commit local passwords, `.env` files or uploaded CVs.

The application uses `spring.jpa.hibernate.ddl-auto=none`; initialize the database separately. If the wishlist table is missing, run `src/main/resources/db/manual/001_saved_jobs.sql` against the application database.

## Run

```text
./mvnw spring-boot:run
```

## Verify Job, Category and Skill workflows

```text
./mvnw test -Dtest=JobDetailControllerTest,JobWorkflowTest,AdminSkillServiceTest,JobPagesTest,JobCategoryAdminServiceTest
```

These tests do not modify the application database. See [JOB_WORKFLOW.md](JOB_WORKFLOW.md) for the business rules and admin integration notes.
