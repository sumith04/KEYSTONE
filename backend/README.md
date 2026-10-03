# KEYSTONE Backend Service

Spring Boot RESTful API backend service for the KEYSTONE Field Service Management Platform.

---

## 🛠️ Tech Stack & Dependencies

- **Java Version:** 21
- **Framework:** Spring Boot 3.3.4
- **Dependencies:**
  - `spring-boot-starter-web`: REST controller routing and HTTP handling
  - `spring-boot-starter-data-jpa`: Relational ORM mapping & repositories
  - `spring-boot-starter-validation`: Bean validation
  - `postgresql`: PostgreSQL JDBC driver
  - `lombok`: Boilerplate reduction (getters, setters, builders)
  - `spring-boot-starter-test`: Testing framework (JUnit 5, AssertJ, Mockito)

---

## 📂 Package Architecture

```
com.keystone
├── config/       # Web CORS, Security & System Beans configuration
├── controller/   # REST Controllers (/api/*)
├── service/      # Business logic service interfaces and implementations
├── repository/   # Spring Data JPA repositories
├── entity/       # Database Entities (@Entity)
├── dto/          # Data Transfer Objects
├── mapper/       # Object Mappers (DTO <-> Entity)
├── exception/    # Custom Exception Handlers (@ControllerAdvice)
└── enums/        # System domain Enums
```

---

## ⚙️ Configuration & Environment Variables

PostgreSQL credentials are read dynamically from environment variables:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/keystone_db}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:}
```

### Local Setup
Export the environment variables or define them in `.env`:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/keystone_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
```

---

## 🚀 Running the Backend

### Compile
```bash
mvn clean compile
```

### Run Application
```bash
mvn spring-boot:run
```

### Verify Endpoint
```bash
curl http://localhost:8080/api/health
```
Expected output:
```json
{
  "status": "UP",
  "application": "KEYSTONE Field Service Management Platform",
  "timestamp": "..."
}
```
