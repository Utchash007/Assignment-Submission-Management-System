# 01 — Project Setup

## Generate the Project

Use [start.spring.io](https://start.spring.io) with:

- **Project**: Maven
- **Language**: Java
- **Spring Boot**: 3.3.x (latest stable)
- **Java**: 21
- **Packaging**: Jar
- **Dependencies**:
  - Spring Web
  - Spring Data JPA
  - PostgreSQL Driver
  - Spring Security
  - Validation
  - Lombok
  - springdoc-openapi-starter-webmvc-ui (add manually, see `pom.xml` below)

## `pom.xml` Essentials

```xml
<properties>
    <java.version>21</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <!-- JWT (replaces System.IdentityModel.Tokens.Jwt) -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <!-- BCrypt (replaces BCrypt.Net-Next; produces identical hashes) -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-crypto</artifactId>
    </dependency>
    <!-- Swagger UI (replaces Scalar) -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.6.0</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## Package Structure

Mirror the .NET folder layout so code reviews map 1:1:

```
backend-spring/src/main/java/com/onnorokom/backend/
├── BackendApplication.java        # @SpringBootApplication (replaces Program.cs)
├── config/                        # replaces Configuration/ + Program.cs wiring
│   ├── SecurityConfig.java        #   filter chain, RBAC, password encoder
│   ├── JwtProperties.java         #   replaces JwtOptions
│   ├── FileUploadProperties.java  #   replaces FileUploadOptions
│   └── OpenApiConfig.java         #   replaces OpenAPI document transformer
├── controller/                    # replaces Controllers/ (10 controllers)
│   ├── AuthController.java
│   ├── UserController.java
│   ├── AcademicTermController.java
│   ├── BatchController.java
│   ├── CourseController.java
│   ├── CourseEnrollmentController.java
│   ├── TeacherCourseAllocationController.java
│   ├── AssignmentController.java
│   ├── SubmissionController.java
│   └── SubmissionAttachmentController.java
├── service/                       # replaces Services/ (10 service pairs → classes)
├── repository/                    # replaces Repository/ + UnitOfWork/
├── entity/                        # replaces Models/Entities/
├── enums/                         # replaces Models/Enums/
├── dto/                           # replaces Models/DTOs/
│   ├── auth/ user/ academicterm/ batch/ course/
│   ├── courseenrollment/ teacherallocation/
│   └── assignment/ submission/ submissionattachment/
├── exception/                     # replaces Middleware/GlobalExceptionMiddleware.cs
│   └── GlobalExceptionHandler.java
├── security/                      # new home for JWT filter, current-user helper
│   ├── JwtAuthenticationFilter.java
│   └── CurrentUser.java
├── seed/                          # replaces Seed/SeedData.cs
│   └── DataSeeder.java
└── util/                          # replaces Helpers/
```

**Note on the `I*Controller` / `I*Service` interfaces:** the .NET codebase has empty abstraction interfaces (e.g. `IAuthController`). Do **not** port them — they add ceremony without value in either language. Keep service interfaces only where you'd want alternate implementations (in practice: none).

## `application.yml` (replaces `.env` + `appsettings.json`)

```yaml
spring:
  application:
    name: onnorokom-backend
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:onnorokom_asm}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate        # schema already exists; never generate
    open-in-view: false         # match EF Core behavior; avoids lazy-loading surprises
    properties:
      hibernate:
        jdbc.time_zone: UTC
  servlet:
    multipart:
      max-file-size: 10MB       # FileUploadOptions parity
      max-request-size: 12MB

server:
  port: ${PORT:5000}

app:
  jwt:
    issuer: ${JWT_ISSUER:OnnoRokomBackend}
    audience: ${JWT_AUDIENCE:OnnoRokomFrontend}
    signing-key: ${JWT_SIGNING_KEY:}       # fail fast if empty (see JwtProperties)
    access-token-lifetime-minutes: 120

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

## Type Mapping Cheat Sheet

| C# / .NET | Java |
|---|---|
| `Guid` | `java.util.UUID` |
| `string` | `String` |
| `string?` | `String` (nullability via `@Nullable` or Optional in services) |
| `DateTime` (timestamptz) | `java.time.Instant` |
| `DateOnly` | `java.time.LocalDate` |
| `decimal` | `java.math.BigDecimal` |
| `byte[]` (bytea) | `byte[]` + `@Column(columnDefinition = "bytea")` |
| `ICollection<T>` nav | `List<T>` (always initialize `= new ArrayList<>()`) |
| `CancellationToken` | nothing — drop the parameter |
| `async Task<T>` | plain `T` (Spring MVC blocking model; JPA is sync) |

## Async Model — Important Mindset Shift

EF Core is async-first (`await ...Async(ct)` everywhere). **Spring MVC + JPA is blocking** — one thread per request, handled efficiently by Tomcat's pool. Translate:

```csharp
public async Task<User?> Get(Guid id, CancellationToken ct)
    => await _repo.Users.FindAsync(id, ct);
```

to:

```java
public Optional<User> get(UUID id) {
    return userRepository.findById(id);
}
```

No `CompletableFuture`, no `@Async` — those are for parallel work, not per-request IO.

## Startup & Seeding (replaces `SeedData.InitializeAsync`)

```java
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    // ... other repositories

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return; // same guard as the .NET seeder
        }
        // seed admin/teacher/student, term FALL2026, batches, courses,
        // enrollments, allocations, assignments, submission — port SeedData.cs verbatim
    }
}
```

## Docker Changes

- `Backend/Dockerfile`: replace `dotnet build/publish` stages with a Maven multi-stage build:
  ```dockerfile
  FROM maven:3.9-eclipse-temurin-21 AS build
  WORKDIR /app
  COPY pom.xml .
  RUN mvn dependency:go-offline
  COPY src ./src
  RUN mvn package -DskipTests

  FROM eclipse-temurin:21-jre
  WORKDIR /app
  COPY --from=build /app/target/*.jar app.jar
  EXPOSE 5000
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```
- `docker-compose.yml`: backend build context moves to the new project dir; env vars (`DB_CONN` → split `DB_HOST`/`DB_NAME`/etc., `Jwt__SigningKey` → `JWT_SIGNING_KEY`) need renaming.
- Render deploy: same shape — Java 21 runtime is heavier than .NET; expect ~150MB more memory.
