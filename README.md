# KEYSTONE - Field Service Management Platform

KEYSTONE is an enterprise-grade Field Service Management (FSM) platform engineered specifically for commercial facility maintenance operations.

> **Status:** Foundation Phase complete. Core architecture, Spring Boot backend structure, React+TypeScript+Vite frontend base, and PostgreSQL database configuration are initialized.

---

## 🛠️ Technology Stack

### Backend
- **Language:** Java 21
- **Framework:** Spring Boot 3.3.4
- **Modules:** Spring Web, Spring Data JPA, Spring Validation
- **Database Driver:** PostgreSQL Driver
- **Utilities:** Lombok
- **Build Tool:** Apache Maven

### Frontend
- **Framework:** React 18
- **Language:** TypeScript
- **Build Tool:** Vite
- **Routing:** React Router v6
- **HTTP Client:** Axios
- **Styling:** Tailwind CSS

### Database
- **Database Engine:** PostgreSQL 15+

---

## 📁 Repository Structure

```
KEYSTONE/
│
├── backend/                  # Spring Boot Java Application
│   ├── pom.xml               # Maven configuration & dependencies
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/keystone/
│   │   │   │   ├── config/   # Security & Web configuration
│   │   │   │   ├── controller/ # REST Controllers
│   │   │   │   ├── service/  # Business service interfaces & implementations
│   │   │   │   ├── repository/ # Spring Data JPA repositories
│   │   │   │   ├── entity/   # JPA Database Entities (Planned)
│   │   │   │   ├── dto/      # Data Transfer Objects (Planned)
│   │   │   │   ├── mapper/   # DTO / Entity Mappers (Planned)
│   │   │   │   ├── exception/# Global exception handling
│   │   │   │   ├── enums/    # System enums
│   │   │   │   └── KeystoneApplication.java
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── application-dev.properties
│   │   └── test/
│   └── README.md
│
├── frontend/                 # React + TypeScript + Vite Application
│   ├── package.json          # Node dependencies & scripts
│   ├── src/
│   │   ├── assets/           # Static assets & images
│   │   ├── components/       # Shared UI components
│   │   ├── hooks/            # Custom React hooks
│   │   ├── layouts/          # Page layouts
│   │   ├── pages/            # Application pages
│   │   ├── routes/           # React Router route definitions
│   │   ├── services/         # API services (Axios client)
│   │   ├── types/            # TypeScript type definitions
│   │   ├── utils/            # Utility functions
│   │   ├── App.tsx           # Main application entry component
│   │   └── main.tsx          # DOM root mount point
│   └── README.md
│
├── .env.example              # Environment variables template
├── .gitignore                # Git exclusion patterns
└── README.md                 # Root documentation
```

---

## ⚙️ Environment Variables

System configuration relies on environment variables rather than hardcoded credentials.

| Variable Name | Description | Default (Dev) |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC Connection String | `jdbc:postgresql://localhost:5432/keystone_db` |
| `DB_USERNAME` | PostgreSQL Database Username | `postgres` |
| `DB_PASSWORD` | PostgreSQL Database Password | *(empty)* |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `SERVER_PORT` | Backend HTTP Port | `8080` |
| `VITE_API_BASE_URL` | Frontend API Base Endpoint | `http://localhost:8080/api` |

Create a `.env` file or export variables in your shell before starting the backend and frontend.

---

## 🐘 PostgreSQL Setup

1. Install and start **PostgreSQL** locally (or via Docker).
2. Create the target database:
   ```sql
   CREATE DATABASE keystone_db;
   ```
3. Set your connection variables via environment variables or `.env`.

---

## 🚀 How to Run the Backend

### Prerequisites
- JDK 21
- Apache Maven 3.8+ (or wrapper)

### Commands
Navigate to the `backend` directory:
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

The Spring Boot backend will start at `http://localhost:8080`.
Verify backend health by calling:
```bash
curl http://localhost:8080/api/health
```

---

## 💻 How to Run the Frontend

### Prerequisites
- Node.js 18+
- npm 9+

### Commands
Navigate to the `frontend` directory:
```bash
cd frontend
npm install
npm run dev
```

The React frontend will start at `http://localhost:5173`.

---

## 📌 Roadmap & Future Business Modules (Planned)

The platform is intentionally structured to cleanly receive the following future business modules in subsequent development phases:

- [ ] **Authentication & Security** (JWT, Spring Security)
- [ ] **Role-Based Access Control (RBAC)** (Admin, Manager, Dispatcher, Technician, Customer)
- [ ] **User & Customer Management**
- [ ] **Site & Asset Tracking**
- [ ] **Service Request Management**
- [ ] **Work Order Lifecycle & Dispatching**
- [ ] **Parts & Inventory Tracking**
- [ ] **Technician Time Tracking**
- [ ] **SLA Monitoring & Compliance**
- [x] **In-app Notifications** (persisted inbox, ownership isolation)
- [x] **Real-Time Notifications & WebSockets** (STOMP over SockJS; JWT CONNECT)
- [x] **Analytical Dashboards & Reports**
- [x] **Customer Self-Service Portal**

---

## Customer Self-Service Portal

CUSTOMER users are scoped to their own customer record via JWT identity (`User.customer`, with email fallback). Request bodies and query parameters never override ownership.

### Lifecycle

`SUBMITTED` → `ACKNOWLEDGED` → `IN_REVIEW` → `CONVERTED_TO_WORK_ORDER`

Terminal alternatives: `SUBMITTED` → `CANCELLED`; `ACKNOWLEDGED`/`IN_REVIEW` → `REJECTED`. Customers cannot set status directly.

### Customer APIs (`/api/customer`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/profile` | Own customer profile (read-only) |
| GET | `/summary` | Dashboard counts |
| GET | `/sites` | Own sites (paginated, read-only) |
| GET | `/requests` | Own service requests (page, search, status, sort) |
| GET/POST/PUT | `/requests`, `/requests/{id}` | Create/view/update own request |
| PATCH | `/requests/{id}/cancel` | Cancel while `SUBMITTED` |
| GET | `/work-orders`, `/work-orders/{id}` | Own work orders + customer-safe SLA fields |

### Internal request APIs (`/api/service-requests`)

ADMIN, MANAGER, and DISPATCHER can list/view requests. Update/acknowledge/review/reject requires `UPDATE_SERVICE_REQUEST`. Conversion (`POST /{id}/convert-to-work-order`) requires `CONVERT_SERVICE_REQUEST` and uses existing `WorkOrderService` in one transaction.

### Role restrictions

CUSTOMER keeps `LOGIN`, `LOGOUT`, `REQUEST_RAISE`, `VIEW_OWN_REQUEST` only. They do **not** receive `VIEW_WORK_ORDER`, `VIEW_CUSTOMER`, or `VIEW_SITE`. Cross-customer IDs return 404. Notifications reuse the existing inbox/WebSocket path.
