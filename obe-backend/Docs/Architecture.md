# System Architecture & Technical Design

## 1. High-Level System Architecture

The **DYPIU NBA Attainment System** follows a clean layered domain-driven Spring Boot architectural model.

```text
[ React Frontend ] (Vite / Client.js)
        │
        ▼ (HTTP REST APIs over JSON)
[ Spring Security Filter Chain ] (JWT Token Filter)
        │
        ▼
[ REST Controllers ] (Controller Layer)
        │
        ▼ (DTOs & Validation)
[ Service Layer ] (Business Logic & Attainment Engines)
        │
        ▼ (JPA Projections / Repositories)
[ Data Access Layer ] (Spring Data JPA / Hibernate)
        │
        ▼ (PostgreSQL Dialect)
[ PostgreSQL Database ] (Managed by Flyway)
```

## 2. Spring Boot Package Organization

```text
com.dypiu.nba
├── ObeBackendApplication.java
├── controller/         # REST API Controllers
├── service/            # Business Logic & Attainment Engines
├── repository/         # Spring Data JPA Repositories
├── entity/             # JPA Entities mapped to PostgreSQL
├── dto/                # Request and Response Transfer Objects
├── security/           # JWT, SecurityConfig, UserDetailsService
├── exception/          # Centralized Exception Handler & Custom Errors
├── config/             # Spring & CORS Configurations
└── util/               # File Storage & Math Helpers
```

## 3. Core Architectural Rules

1. **Java 21 Standards**: Uses Java 21 features (Records, Pattern Matching, Sealed Types where appropriate).
2. **PostgreSQL Only**: No secondary databases allowed. All queries run against PostgreSQL.
3. **Flyway Migration Authority**: Database DDL modifications (`CREATE`, `ALTER`, `INDEX`) are managed strictly through Flyway scripts in `classpath:db/migration`.
4. **Clean Controller Boundary**: Controllers handle request mapping, DTO validation, and response wrapping. Business logic remains strictly inside services.
5. **No N+1 Queries**: JPA queries utilize joins or entity graphs to prevent performance degradation on large datasets.
