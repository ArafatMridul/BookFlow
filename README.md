# BookFlow — Library Management System

> A full-stack library management web application built with Spring Boot, enabling multi-role users (Admin, Librarian, Member) to manage books, borrowings, renewals, fines, and wishlists through a unified platform.

**Live Demo:** [https://bookflow-w9rh.onrender.com/login]
> _Note: The app is hosted on Render's free tier. It may take 30–60 seconds to wake up on the first visit._

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [System Architecture](#3-system-architecture)
4. [Project Structure](#4-project-structure)
5. [Database Design](#5-database-design)
6. [Features & Functionality](#6-features--functionality)
7. [Authentication & Authorization](#7-authentication--authorization)
8. [API Endpoints](#8-api-endpoints)
9. [Running the Project](#9-running-the-project)
10. [Testing](#10-testing)
11. [CI/CD Pipeline](#11-cicd-pipeline)
12. [Deployment on Render](#12-deployment-on-render)
13. [Default Credentials](#13-default-credentials)

---

## 1. Project Overview

**BookFlow** is a role-based Library Management System where users can browse and borrow books, librarians manage inventory and borrowing requests, and admins oversee the entire system. The project demonstrates a production-grade Spring Boot application with:

- Multi-role authentication (Admin / Librarian / Member)
- Dual authentication modes — session-based web login and JWT-based API access
- A complete borrowing lifecycle with renewal tokens and fine calculation
- A book wishlist system
- CI/CD pipeline with GitHub Actions and cloud deployment on Render

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.11 |
| **Build Tool** | Maven 3.9.9 |
| **Web Layer** | Spring MVC + Thymeleaf 3 |
| **Security** | Spring Security 6 + JWT (JJWT 0.11.5) |
| **Database** | PostgreSQL 16 |
| **ORM** | Spring Data JPA / Hibernate |
| **Test DB** | H2 (in-memory, test scope only) |
| **Frontend** | Bootstrap 5.3.2, Font Awesome 6.5.1, jQuery 3.7.1 |
| **Containerization** | Docker (multi-stage build) |
| **CI/CD** | GitHub Actions |
| **Deployment** | Render |

---

## 3. System Architecture

BookFlow follows a classic **Layered MVC Architecture** common to enterprise Spring Boot applications.

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (Browser)                        │
│              HTML + Thymeleaf + Bootstrap + jQuery              │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP Requests
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Security Filter Chain                 │
│         (JWT Filter + Session Auth + Role-based Access)         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Controller Layer                          │
│  HomeController  │  AuthController  │  AdminController          │
│  LibrarianController  │  UserController  │  AuthApiController    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Service Layer                            │
│         AuthService  │  UserService  │  CustomUserDetailsService │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Repository Layer                           │
│    UserRepository  │  BookRepository  │  BorrowingRepository    │
│                    RoleRepository                               │
└────────────────────────────┬────────────────────────────────────┘
                             │ Spring Data JPA
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                 PostgreSQL 16 Database                          │
│       users │ roles │ books │ borrowings │ wishlists            │
└─────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

- **Hybrid Authentication:** Web users authenticate via session (JSESSIONID cookie); API consumers use JWT tokens stored in an `HttpOnly` cookie — both coexist in the same Spring Security configuration.
- **Role-based Redirects:** After login, users are automatically redirected to their role-specific dashboard (Admin → `/admin/dashboard`, Librarian → `/librarian/dashboard`, User → `/user/dashboard`).
- **DTO Pattern:** Data Transfer Objects separate the API/view layer from JPA entities, preventing accidental data exposure.
- **Soft Disabling:** Accounts are disabled (not deleted) to preserve referential integrity in borrowing history.

---

## 4. Project Structure

```
BookFlow/
├── .github/
│   └── workflows/
│       ├── ci-cd.yml                        # Build, test, deploy pipeline
│       └── branch-protection-setup.yml      # Branch rules
│
└── bookflowproject/                         # Main Spring Boot module
    ├── src/
    │   ├── main/
    │   │   ├── java/com/example/bookflowproject/
    │   │   │   ├── config/                  # Security & app configuration
    │   │   │   │   ├── SecurityConfig.java
    │   │   │   │   └── CustomAuthSuccessHandler.java
    │   │   │   │
    │   │   │   ├── controller/              # Request handlers
    │   │   │   │   ├── AuthApiController.java     (REST: login, signup)
    │   │   │   │   ├── AuthController.java        (Web: login/register forms)
    │   │   │   │   ├── HomeController.java        (/, /dashboard routing)
    │   │   │   │   ├── AdminController.java       (/admin/**)
    │   │   │   │   ├── LibrarianController.java   (/librarian/**)
    │   │   │   │   ├── UserController.java        (/user/**)
    │   │   │   │   └── DashboardController.java
    │   │   │   │
    │   │   │   ├── dto/                     # Data Transfer Objects
    │   │   │   │   ├── LoginRequest.java
    │   │   │   │   ├── SignupRequest.java
    │   │   │   │   └── UserDTO.java
    │   │   │   │
    │   │   │   ├── entity/                  # JPA Entities (DB tables)
    │   │   │   │   ├── User.java
    │   │   │   │   ├── Role.java
    │   │   │   │   ├── Book.java
    │   │   │   │   └── Borrowing.java
    │   │   │   │
    │   │   │   ├── error/                   # Global exception handling
    │   │   │   │
    │   │   │   ├── repository/              # Data access (Spring Data JPA)
    │   │   │   │   ├── UserRepository.java
    │   │   │   │   ├── RoleRepository.java
    │   │   │   │   ├── BookRepository.java
    │   │   │   │   └── BorrowingRepository.java
    │   │   │   │
    │   │   │   ├── security/                # JWT utility classes
    │   │   │   │   ├── JwtUtils.java
    │   │   │   │   └── JwtAuthFilter.java
    │   │   │   │
    │   │   │   └── services/                # Business logic
    │   │   │       ├── AuthService.java
    │   │   │       ├── UserService.java
    │   │   │       └── CustomUserDetailsService.java
    │   │   │
    │   │   └── resources/
    │   │       ├── templates/               # Thymeleaf HTML templates (17 files)
    │   │       │   ├── home.html
    │   │       │   ├── login.html
    │   │       │   ├── register.html
    │   │       │   ├── admin/
    │   │       │   │   ├── dashboard.html
    │   │       │   │   ├── users.html
    │   │       │   │   ├── books.html
    │   │       │   │   └── reports.html
    │   │       │   ├── librarian/
    │   │       │   │   ├── dashboard.html
    │   │       │   │   ├── books.html
    │   │       │   │   ├── patrons.html
    │   │       │   │   └── borrowings.html
    │   │       │   └── user/
    │   │       │       ├── dashboard.html
    │   │       │       ├── catalog.html
    │   │       │       ├── book-detail.html
    │   │       │       ├── my-borrowings.html
    │   │       │       └── wishlist.html
    │   │       ├── static/                  # CSS, JS, images
    │   │       ├── application.properties   # App configuration
    │   │       └── data.sql                 # Seed data (roles, default users)
    │   │
    │   └── test/
    │       └── java/com/example/bookflowproject/
    │           ├── controllers/             # Controller unit & integration tests
    │           └── services/                # Service unit tests
    │
    ├── pom.xml                              # Maven dependencies
    ├── Dockerfile                           # Multi-stage Docker build
    └── docker-compose.yml                   # Local PostgreSQL container
```

---

## 5. Database Design

The system uses **6 relational tables** in PostgreSQL:

### Entity-Relationship Diagram

```
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│    roles     │        │    users     │        │    books     │
├──────────────┤        ├──────────────┤        ├──────────────┤
│ id (PK)      │◄──┐    │ id (PK)      │    ┌──►│ id (PK)      │
│ name         │   │    │ username     │    │   │ title        │
└──────────────┘   │    │ email        │    │   │ author       │
                   │    │ password     │    │   │ isbn         │
┌──────────────┐   │    │ first_name   │    │   │ publisher    │
│ user_roles   │   │    │ last_name    │    │   │ pub_year     │
├──────────────┤   │    │ enabled      │    │   │ cover_url    │
│ user_id (FK) │───┘    └──────┬───────┘    │   │ total_copies │
│ role_id (FK) │               │            │   │ avail_copies │
└──────────────┘               │            │   └──────────────┘
                               │            │
                   ┌───────────▼────────────┴──┐
                   │        borrowings          │
                   ├────────────────────────────┤
                   │ id (PK)                    │
                   │ user_id (FK)               │
                   │ book_id (FK)               │
                   │ borrow_date                │
                   │ due_date                   │
                   │ return_date                │
                   │ status                     │
                   │ renewal_tokens_acquired    │
                   │ renewal_tokens_used        │
                   │ current_period_start       │
                   │ fine_amount                │
                   │ fine_cleared               │
                   └────────────────────────────┘

┌──────────────────┐
│  user_wishlists  │
├──────────────────┤
│ user_id (FK)     │──► users
│ book_id (FK)     │──► books
└──────────────────┘
```

### Table Descriptions

| Table | Purpose |
|---|---|
| `users` | Stores member, librarian, and admin accounts |
| `roles` | Defines the three roles: ADMIN, LIBRARIAN, USER |
| `user_roles` | Many-to-many junction — a user can have multiple roles |
| `books` | Book catalog with inventory tracking (total vs available copies) |
| `borrowings` | Full borrowing lifecycle including renewals and fine tracking |
| `user_wishlists` | Many-to-many — books saved to a user's personal wishlist |

### Borrowing Status State Machine

```
[User Requests Borrow]
        │
        ▼
  [REQUESTED] ─────────────────────────────► [REJECTED]
        │                                  (Librarian rejects)
        │ Librarian Approves
        ▼
   [BORROWED]
        │
        ├── Past due date? ─── Yes ──► [OVERDUE]
        │                                  │
        │◄─────────────────────────────────┘
        │
        │ User submits return request
        ▼
[RETURN_REQUESTED]
        │
        ├── Librarian Processes ──► [RETURNED]
        │
        └── Librarian Rejects ───► Back to [BORROWED]
```

---

## 6. Features & Functionality

### 6.1 Member (USER Role)
- Register and log in to their personal account
- Browse the full book catalog with search by title or author
- View book details and request to borrow a book
- View personal borrowing history and current status
- Renew a borrowed book using **renewal tokens** (up to 3 tokens per borrow, each adds 7 days)
- Submit a return request when done reading
- View and clear outstanding fines (simulated payment)
- Manage a personal wishlist — add or remove books

### 6.2 Librarian (LIBRARIAN Role)
- View and manage the full book inventory
- Add new books, edit book details, or delete books (only if no active borrowings exist)
- Track available vs total copies for each book
- View all borrowing requests and approve or reject them
- Process member return requests or reject them if needed
- Search and view registered member (patron) profiles

### 6.3 Admin (ADMIN Role)
- View system-wide statistics: total users, books, librarians, members
- Manage all user accounts — search, view details, enable or disable accounts
- View active loans, pending requests, overdue items, and recent returns
- Has all librarian permissions in addition to admin-only controls

### 6.4 Fine and Renewal System (Key Business Logic)

The borrowing system implements a dynamic return policy:

- When a book is borrowed, the member receives **0 to 3 renewal tokens** (configurable)
- Each token can be used to extend the due date by **7 days**
- When a member attempts to renew or return after the due date, a **fine is calculated** at 1 tk per overdue day and **locked in** (committed)
- The member must **clear their fine** (simulated payment) before returning the book
- Once returned, available copies are incremented back in the book inventory

---

## 7. Authentication & Authorization

### Authentication Modes

BookFlow supports two parallel authentication flows:

| Mode | Mechanism | Used For |
|---|---|---|
| **Form Login** | Spring Security session (JSESSIONID cookie) | Web browser access |
| **JWT Login** | HMAC-SHA256 signed token in HttpOnly cookie | REST API clients |

**JWT Configuration:**
- Algorithm: HMAC-SHA256
- Expiry: 24 hours (86,400,000 ms)
- Storage: HttpOnly, Secure, SameSite=Lax cookie

### Role-Based Access Control

```
URL Pattern              Allowed Roles
────────────────────────────────────────────────────────────
/admin/**                ROLE_ADMIN only
/librarian/**            ROLE_ADMIN, ROLE_LIBRARIAN
/user/**                 ROLE_USER, ROLE_LIBRARIAN, ROLE_ADMIN
/api/admin/**            ROLE_ADMIN only
/api/librarian/**        ROLE_ADMIN, ROLE_LIBRARIAN
/api/user/**             Any authenticated user
/login, /register        Public (unauthenticated access)
/                        Public
```

### Security Features
- Passwords hashed with **BCrypt**
- **CSRF protection** enabled (exempted only for login, logout, and API routes)
- Account **disabling** without deletion (preserves audit history)
- Role-based **login success handler** that redirects users to their dashboard
- Proxy header trust configured for cloud deployment (Render)

---

## 8. API Endpoints

### REST API

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Authenticate and receive JWT token |
| `POST` | `/api/auth/signup` | Public | Register a new member account |
| `GET` | `/api/user/profile` | Authenticated | Get current user profile |
| `GET` | `/api/user/{id}` | Admin/Librarian | Get user by ID |
| `GET` | `/api/user` | Admin only | Get all users |

### Web Routes

**Home & Auth**

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Landing/home page |
| `GET` | `/login` | Login form |
| `POST` | `/login` | Submit login credentials |
| `GET` | `/register` | Registration form |
| `POST` | `/register` | Submit registration |
| `GET` | `/dashboard` | Redirects to role-appropriate dashboard |

**Admin Routes (`/admin/**`)**

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/admin/dashboard` | Admin overview with statistics |
| `GET` | `/admin/users` | User management list |
| `POST` | `/admin/users/{id}/toggle-enabled` | Enable or disable a user account |
| `GET` | `/admin/books` | Book management page |
| `GET` | `/admin/reports` | System reports (loans, overdue, returns) |

**Librarian Routes (`/librarian/**`)**

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/librarian/dashboard` | Librarian overview |
| `GET` | `/librarian/books` | View and search book inventory |
| `GET` | `/librarian/books/add` | Add book form |
| `POST` | `/librarian/books/add` | Submit new book |
| `POST` | `/librarian/books/{id}/update` | Update book details |
| `POST` | `/librarian/books/{id}/delete` | Delete a book |
| `GET` | `/librarian/patrons` | Search patron/member profiles |
| `GET` | `/librarian/borrowings` | View all borrowing requests |
| `POST` | `/librarian/borrowings/{id}/approve` | Approve borrow request |
| `POST` | `/librarian/borrowings/{id}/reject` | Reject borrow request |
| `POST` | `/librarian/borrowings/{id}/process-return` | Process a return |
| `POST` | `/librarian/borrowings/{id}/reject-return` | Reject a return request |

**User Routes (`/user/**`)**

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/user/dashboard` | Member personal dashboard |
| `GET` | `/user/catalog` | Browse book catalog |
| `GET` | `/user/catalog/{bookId}` | View book details |
| `POST` | `/user/catalog/{bookId}/request-borrow` | Submit borrow request |
| `GET` | `/user/my-borrowings` | View personal borrowing history |
| `POST` | `/user/my-borrowings/{id}/renew` | Renew a book with a token |
| `POST` | `/user/my-borrowings/{id}/clear-fine` | Clear an outstanding fine |
| `POST` | `/user/my-borrowings/{id}/request-return` | Submit return request |
| `GET` | `/user/wishlist` | View personal wishlist |
| `POST` | `/user/wishlist/add/{bookId}` | Add book to wishlist |
| `POST` | `/user/wishlist/remove/{bookId}` | Remove book from wishlist |

---

## 9. Running the Project

### Prerequisites

- Java 21 (Eclipse Temurin or any JDK 21)
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- Docker & Docker Compose (for local PostgreSQL)
- Git

### Step 1 — Clone the Repository

```bash
git clone https://github.com/ArafatMridul/BookFlow.git
cd BookFlow/bookflowproject
```

### Step 2 — Start the Database

```bash
docker-compose up -d
```

This spins up a PostgreSQL 16 container on port `5433` with database `bookflow`.

### Step 3 — Run the Application

```bash
./mvnw spring-boot:run
```

Or build the JAR first:

```bash
./mvnw clean package -DskipTests
java -jar target/bookflowproject-0.0.1-SNAPSHOT.jar
```

### Step 4 — Open in Browser

```
http://localhost:8085
```

### Environment Variables (for production or Render deployment)

| Variable | Description | Default |
|---|---|---|
| `JDBC_DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5433/bookflow` |
| `JDBC_DATABASE_USERNAME` | Database username | `postgres` |
| `JDBC_DATABASE_PASSWORD` | Database password | `admin` |
| `JWT_SECRET` | Base64 secret (min 256-bit) | See `application.properties` |
| `PORT` | Server port | `8085` |

### Docker Build

```bash
docker build -t bookflow:latest .

docker run -p 8080:8080 \
  -e JDBC_DATABASE_URL=jdbc:postgresql://host:5432/bookflow \
  -e JDBC_DATABASE_USERNAME=postgres \
  -e JDBC_DATABASE_PASSWORD=yourpassword \
  bookflow:latest
```

---

## 10. Testing

The project uses **JUnit 5**, **Mockito**, and **MockMvc** for a comprehensive test suite covering all layers.

### Test Structure

```
test/
├── controllers/
│   ├── AdminControllerTest.java                    # Admin route unit tests
│   ├── AdminControllerIntegrationTest.java         # Admin integration tests
│   ├── AuthApiControllerTest.java                  # REST auth API tests
│   ├── AuthControllerTest.java                     # Login/register form tests
│   ├── CatalogControllerTest.java                  # Catalog browsing tests
│   ├── CatalogSecurityIntegrationTest.java         # Catalog access control tests
│   ├── DashboardControllerTest.java                # Dashboard routing tests
│   ├── HomeControllerTest.java                     # Home page tests
│   ├── LibrarianControllerTest.java                # Librarian operations tests
│   ├── UserControllerTest.java                     # Member operations tests
│   └── UserControllerSecurityIntegrationTest.java  # Member access control tests
└── services/
    ├── AuthServiceTest.java                        # Auth business logic tests
    ├── CustomUserDetailsServiceTest.java           # UserDetails loading tests
    └── UserServiceTest.java                        # User management tests
```

### Testing Approach

| Test Type | Tools Used | Purpose |
|---|---|---|
| **Unit Tests** | `@WebMvcTest` + `@MockitoBean` | Test controllers in isolation |
| **Integration Tests** | `@SpringBootTest` + `MockMvc` | Full Spring context testing |
| **Service Tests** | JUnit 5 + Mockito | Business logic validation |
| **Security Tests** | `@WithMockUser` + MockMvc | Role-based access control verification |

### Run Tests

```bash
# Run all tests
./mvnw clean test

# Run a specific test class
./mvnw test -Dtest=AuthServiceTest

# Run with full report
./mvnw clean verify
```

> **Note:** Tests use an **H2 in-memory database** (automatically configured for `test` scope), so no running PostgreSQL instance is needed to execute the test suite.

---

## 11. CI/CD Pipeline

The project uses **GitHub Actions** for fully automated build, test, and deployment. The pipeline is defined in [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml).

### Pipeline Flow

```
┌────────────────────────────────────────────────────────────────┐
│                     GitHub Actions Pipeline                    │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  TRIGGER                                                       │
│  ├── Pull Request → main or ci-cd branch                      │
│  └── Push → main branch                                       │
│                                                                │
│  JOB 1: test  (runs on every PR and push)                     │
│  ├── Step 1: Checkout code (actions/checkout@v4)              │
│  ├── Step 2: Setup JDK 21 — Eclipse Temurin distribution      │
│  ├── Step 3: Cache ~/.m2 Maven repository                     │
│  ├── Step 4: mvn clean compile  ← fails fast on build errors  │
│  └── Step 5: mvn test           ← runs full test suite        │
│                                                                │
│  JOB 2: deploy  (runs ONLY on push to main, after test pass)  │
│  └── Step 6: curl POST to RENDER_DEPLOY_HOOK secret           │
│              → triggers Render to rebuild and redeploy        │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### What Each Job Does

**`test` job** — runs on every pull request and every push to `main`:
- Checks out the repository inside `bookflowproject/` working directory
- Installs JDK 21 and caches the Maven dependency cache between runs to speed up builds
- Compiles the application (`mvn clean compile`) — catches syntax errors and missing dependencies early
- Runs the full test suite (`mvn test`) — uses H2 in-memory database so no external services are needed

**`deploy` job** — runs only after `test` passes, only on `main` branch pushes:
- Uses `needs: test` to enforce sequential dependency (will not run if tests fail)
- Sends an HTTP POST to the `RENDER_DEPLOY_HOOK` secret stored in GitHub repository settings
- This webhook triggers Render to pull the latest code, rebuild the Docker image, and deploy the new version

### Branch Protection

Branch protection rules are enforced on `main` — pull requests **cannot be merged** unless the CI pipeline passes. This prevents broken code from reaching production.

---

## 12. Deployment on Render

BookFlow is deployed as a web service on **[Render](https://render.com)** — a cloud platform with native Docker and PostgreSQL support.

**Live URL:** [https://bookflow-latest.onrender.com](https://bookflow-latest.onrender.com)

### How Deployment Works

```
Developer pushes to main branch
         │
         ▼
GitHub Actions CI runs (build + test)
         │
         │ All tests pass
         ▼
GitHub Actions triggers Render Deploy Hook (HTTP POST)
         │
         ▼
Render pulls latest code → builds Docker image
         │
         ▼
Render starts new container → routes traffic
         │
         ▼
Live at: https://bookflow-latest.onrender.com
```

### Render Service Configuration

| Setting | Value |
|---|---|
| **Service Type** | Web Service |
| **Runtime** | Docker |
| **Branch** | `main` |
| **Build Command** | Docker multi-stage build (Maven + JDK 21) |
| **Port** | `8080` (mapped from `PORT` env variable) |
| **Database** | Render Managed PostgreSQL |
| **Auto-Deploy** | Triggered via GitHub Actions webhook |
| **Deploy Trigger** | `RENDER_DEPLOY_HOOK` secret in GitHub |

### Render Environment Variables

These are configured in the Render dashboard under **Environment**:

| Variable | Description |
|---|---|
| `JDBC_DATABASE_URL` | Render PostgreSQL internal connection URL |
| `JDBC_DATABASE_USERNAME` | Database username |
| `JDBC_DATABASE_PASSWORD` | Database password |
| `JWT_SECRET` | HMAC-SHA256 secret key (min 256-bit) |
| `PORT` | `8080` (set by Render automatically) |
| `SPRING_PROFILES_ACTIVE` | `production` |

### Dockerfile (Multi-Stage Build)

The app uses a two-stage Docker build to keep the final image small:

```dockerfile
# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run with JRE only
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> **Free tier note:** Render's free tier spins down idle services. The first request after inactivity may take 30–60 seconds to respond.

---

## 13. Default Credentials

The `data.sql` seed file creates the following accounts on first startup:

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Librarian | `librarian` | `librarian123` |
| Member | `user` | `user123` |

> These are for local development only. Change all credentials before any production deployment.

---

## Project Summary

BookFlow demonstrates real-world software engineering practices including:

- **Layered architecture** (Controller → Service → Repository → Database)
- **Security best practices** (BCrypt, JWT, RBAC, CSRF, HttpOnly cookies)
- **Domain-driven design** with a rich borrowing lifecycle (tokens, fines, state machine)
- **Hybrid web + REST API** architecture serving both browser and API clients
- **Automated testing** across all layers with unit and integration coverage
- **Containerized deployment** with Docker and cloud-ready configuration
- **CI/CD automation** with GitHub Actions ensuring code quality on every push

