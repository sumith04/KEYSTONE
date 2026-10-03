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

---

## Notifications

In-app notifications are persisted in PostgreSQL (`notifications`) and scoped to the authenticated recipient. Clients never supply a recipient user ID.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/notifications` | Paginated inbox. Query: `page`, `size`, `read`, `type`. Newest first. |
| GET | `/api/notifications/unread-count` | `{ "unreadCount": n }` using COUNT, not a full load. |
| PATCH | `/api/notifications/{id}/read` | Idempotent. Another user's id returns 404. |
| PATCH | `/api/notifications/read-all` | Bulk update for the current user only. |

Viewing one's own inbox requires authentication only. `SEND_NOTIFICATION` is unchanged and is **not** required to read personal notifications.

Automatic triggers (failures are isolated and do not roll back the business operation):

- Work order assigned to a new technician → `WORK_ORDER_ASSIGNED`
- Start / hold / resume → `WORK_ORDER_STATUS_CHANGED` for the assigned technician
- Completed / closed / cancelled → matching type for the technician plus enabled ADMIN/MANAGER users
- Stock usage at or below reorder level → `PART_LOW_STOCK` for ADMIN/MANAGER (deduped while unread)
- `NotificationService.notifySlaIfNeeded(workOrder)` creates `SLA_AT_RISK` / `SLA_BREACHED` once per recipient/type/work order. KEYSTONE has no scheduler yet, so this is a hook for future monitoring rather than live SLA polling.
- `SERVICE_REQUEST` is reserved for the Customer Portal (Prompt 13). No service-request module exists here.

WebSocket / STOMP real-time delivery is layered on top of this REST inbox. The database remains the source of truth. Delivery failures are logged and never roll back the persisted notification. Retention cleanup is not scheduled; add a purge job later if needed.

### Real-time delivery

| Item | Value |
|---|---|
| Handshake | `/ws` (SockJS) |
| Application prefix | `/app` |
| User destination | `/user/queue/notifications` |
| Auth | STOMP CONNECT `Authorization: Bearer <JWT>` |

Clients may only subscribe to `/user/queue/notifications`. They cannot publish notifications. Handshake `/ws/**` is permitted so the browser can upgrade; the ChannelInterceptor rejects CONNECT without a valid JWT.
