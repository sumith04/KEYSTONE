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
- [ ] **Analytical Dashboards & Reports**
- [ ] **Customer Self-Service Portal**
