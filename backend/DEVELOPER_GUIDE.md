# Developer Quick Start Guide

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- PostgreSQL 14+ with pgvector extension
- Node.js 18+ (for frontend)
- OpenAI API key
- (Optional) Twilio account for voice features
- (Optional) SMTP server for email

## Setup Steps

### 1. Database Setup

```bash
# Install PostgreSQL
brew install postgresql@14  # macOS
# or
sudo apt install postgresql-14  # Linux

# Start PostgreSQL
brew services start postgresql@14  # macOS
# or
sudo systemctl start postgresql  # Linux

# Create database and user
psql postgres
CREATE DATABASE insurance_db;
CREATE USER insurance_user WITH PASSWORD 'change_this_password';
GRANT ALL PRIVILEGES ON DATABASE insurance_db TO insurance_user;
\c insurance_db
CREATE EXTENSION vector;
\q
```

### 2. Environment Configuration

Create `.env` file in backend directory:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/insurance_db
SPRING_DATASOURCE_USERNAME=insurance_user
SPRING_DATASOURCE_PASSWORD=change_this_password

# Security
JWT_SECRET=your-jwt-secret-key-min-256-bits-change-this-in-production

# OpenAI
OPENAI_API_KEY=sk-your-api-key-here
OPENAI_TEXT_MODEL=gpt-4o
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
OPENAI_REALTIME_MODEL=gpt-4o-realtime-preview-2024-12-17

# Twilio (optional)
TWILIO_ACCOUNT_SID=your-account-sid
TWILIO_AUTH_TOKEN=your-auth-token
TWILIO_PHONE_NUMBER=+1234567890

# SMTP Email (optional)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_FROM=noreply@insurance-app.com

# Application
VOICE_MOCK_MODE=true
STORAGE_TYPE=local
STORAGE_LOCAL_PATH=./storage
FRONTEND_URL=http://localhost:3000
```

### 3. Build and Run

```bash
cd backend

# Install dependencies
mvn clean install

# Run database migrations
mvn flyway:migrate

# Start application
mvn spring-boot:run

# Or build JAR and run
mvn package
java -jar target/insurance-app-backend-1.0.0.jar
```

Application will start on `http://localhost:8080`

### 4. Verify Installation

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}

# Check Swagger UI
open http://localhost:8080/swagger-ui.html
```

## Development Workflow

### Running in Development Mode

```bash
# Enable auto-reload with Spring DevTools
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=CallTaskServiceTest

# Run with coverage
mvn test jacoco:report
```

### Database Migrations

```bash
# Create new migration
# Create file: src/main/resources/db/migration/V{version}__{description}.sql

# Example: V6__add_new_table.sql

# Apply migrations
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Clean database (WARNING: deletes all data)
mvn flyway:clean
```

## Module Structure

Each module follows this structure:

```
com.insurance.{module}/
├── controller/       # REST API endpoints
├── service/         # Business logic
├── repository/      # Data access
├── dto/            # Data transfer objects
├── mapper/         # Entity <-> DTO mappers (optional)
└── exception/      # Custom exceptions (optional)
```

## Common Development Tasks

### Add New Entity

1. Create entity in `com.insurance.common.entity`:
```java
@Entity
@Table(name = "my_table")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // fields...
}
```

2. Create migration:
```sql
-- V7__create_my_table.sql
CREATE TABLE my_table (
    id BIGSERIAL PRIMARY KEY,
    -- columns...
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

3. Create repository:
```java
@Repository
public interface MyRepository extends JpaRepository<MyEntity, Long> {
    // custom queries
}
```

### Add New Endpoint

1. Create DTO:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyDto {
    private Long id;
    // fields...
}
```

2. Create service method:
```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final MyRepository repository;

    @Transactional
    public MyDto createSomething(CreateRequest request) {
        // logic...
    }
}
```

3. Create controller endpoint:
```java
@RestController
@RequestMapping("/api/my-resource")
@RequiredArgsConstructor
public class MyController {
    private final MyService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MyDto> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.ok(service.createSomething(request));
    }
}
```

## Testing

### Unit Test Example

```java
@SpringBootTest
class MyServiceTest {

    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyService service;

    @Test
    void testCreate() {
        // Arrange
        CreateRequest request = CreateRequest.builder()
            .field("value")
            .build();

        MyEntity entity = MyEntity.builder()
            .id(1L)
            .build();

        when(repository.save(any())).thenReturn(entity);

        // Act
        MyDto result = service.createSomething(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
}
```

### Integration Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
class MyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateEndpoint() throws Exception {
        String requestJson = """
            {
                "field": "value"
            }
            """;

        mockMvc.perform(post("/api/my-resource")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists());
    }
}
```

## Debugging

### Enable SQL Logging

In `application.yml`:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Enable Request Logging

```yaml
logging:
  level:
    org.springframework.web: DEBUG
```

### Remote Debugging

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

Then attach debugger to port 5005.

## Common Issues & Solutions

### Issue: Database connection failed
**Solution:** Check PostgreSQL is running and credentials are correct
```bash
psql -U insurance_user -d insurance_db -h localhost
```

### Issue: Flyway migration failed
**Solution:** Check migration files and fix, then:
```bash
mvn flyway:repair
mvn flyway:migrate
```

### Issue: OpenAI API rate limit
**Solution:** Add retry logic or reduce concurrent requests:
```java
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
public String callOpenAI(String prompt) {
    // API call
}
```

### Issue: Port 8080 already in use
**Solution:** Change port in `application.yml`:
```yaml
server:
  port: 8081
```

### Issue: Mock mode not working for voice
**Solution:** Set environment variable:
```bash
export VOICE_MOCK_MODE=true
```

## Performance Tips

### 1. Enable Database Connection Pooling

Already configured in `application.yml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

### 2. Add Caching

```java
@Cacheable("products")
public Product getProduct(Long id) {
    return repository.findById(id).orElseThrow();
}
```

### 3. Optimize Queries

Use `@EntityGraph` to avoid N+1 queries:
```java
@EntityGraph(attributePaths = {"category", "documents"})
Product findById(Long id);
```

### 4. Async Processing

```java
@Async
public CompletableFuture<Void> indexProductsAsync() {
    indexAllProducts();
    return CompletableFuture.completedFuture(null);
}
```

## Security Best Practices

1. **Never commit secrets** - Use environment variables
2. **Use HTTPS in production** - Configure SSL certificate
3. **Rate limiting** - Add rate limiting for public endpoints
4. **Input validation** - Use `@Valid` on all request bodies
5. **SQL injection** - Use parameterized queries (JPA handles this)
6. **XSS protection** - Enable in Spring Security config
7. **CSRF protection** - Enabled by default for non-API endpoints

## Deployment Checklist

- [ ] Update `JWT_SECRET` to strong random value
- [ ] Set `VOICE_MOCK_MODE=false` for production
- [ ] Configure production database
- [ ] Set up SMTP server
- [ ] Configure Twilio credentials
- [ ] Set `LOG_LEVEL=INFO` or `WARN`
- [ ] Enable HTTPS
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure backup strategy
- [ ] Set up CI/CD pipeline
- [ ] Review and update CORS settings
- [ ] Enable rate limiting
- [ ] Set up error tracking (Sentry, etc.)

## Useful Commands

```bash
# View logs
tail -f logs/insurance-app.log

# Check running processes
lsof -i :8080

# Database backup
pg_dump -U insurance_user insurance_db > backup.sql

# Database restore
psql -U insurance_user insurance_db < backup.sql

# Generate OpenAPI spec
curl http://localhost:8080/api-docs > openapi.json

# Load test endpoint
ab -n 1000 -c 10 http://localhost:8080/api/products
```

## IDE Setup

### IntelliJ IDEA

1. Install Lombok plugin
2. Enable annotation processing: Settings -> Build -> Compiler -> Annotation Processors
3. Import as Maven project
4. Configure code style: Use Google Java Style Guide

### VS Code

1. Install extensions:
   - Language Support for Java
   - Spring Boot Extension Pack
   - Lombok Annotations Support
2. Configure `settings.json`:
```json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.compile.nullAnalysis.mode": "automatic"
}
```

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)
- [PostgreSQL pgvector](https://github.com/pgvector/pgvector)
- [Project README](../README.md)
- [API Endpoints](./API_ENDPOINTS.md)
- [Implementation Summary](./IMPLEMENTATION_SUMMARY.md)

## Support

For issues or questions:
1. Check existing documentation
2. Search GitHub issues
3. Create new issue with:
   - Description of problem
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details
   - Relevant logs

## License

[Add your license information here]
