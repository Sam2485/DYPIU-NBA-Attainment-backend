# DYPIU NBA Attainment Backend Documentation

Welcome to the backend documentation for the **DYPIU NBA Attainment System**, a production-grade academic outcome attainment and continuous quality improvement platform designed for **D. Y. Patil International University (DYPIU)**.

## Architecture Overview

The system is engineered as a **Java 21 + Spring Boot 3.3.2** backend connected strictly to **PostgreSQL**. Database schema evolution is 100% managed via versioned **Flyway** migrations.

### Technology Stack
- **Java 21 LTS**
- **Spring Boot 3.3.2**
- **Spring Data JPA / Hibernate**
- **Spring Security + JWT (JSON Web Tokens)**
- **Flyway Database Migration Engine**
- **PostgreSQL Database**
- **Jakarta Bean Validation**
- **Apache POI** (Excel template processing & mark upload handling)

---

## Quick Sitemap of Backend Documentation

1. [Architecture.md](Architecture.md) — Architectural design, clean request flow, package layout, layer separation.
2. [Database.md](Database.md) — PostgreSQL ER design, tables, constraints, foreign keys, indexes, and Flyway migration strategy.
3. [API-Documentation.md](API-Documentation.md) — Complete REST API catalog, endpoints, HTTP methods, request/response DTOs.
4. [Authentication.md](Authentication.md) — JWT authentication flow, token generation, login session & OTP flow.
5. [Authorization.md](Authorization.md) — Role-based access control (IQAC, Director, HOD, Programme Coordinator, Faculty).
6. [Deployment.md](Deployment.md) — Local server VM deployment guide, systemd service, nginx proxy configuration.
7. [Configuration.md](Configuration.md) — `application.yml` parameters, database pooling, JPA properties.
8. [File-Storage.md](File-Storage.md) — Local server disk storage specification for marksheets and attachments.
9. [Error-Handling.md](Error-Handling.md) — Centralized exception handler and standardized error JSON schemas.
10. [Testing.md](Testing.md) — Unit testing, formula validation, context loading, and Flyway migration tests.
11. [Development-Guide.md](Development-Guide.md) — Developer setup instructions, Maven commands, IDE configuration.
12. [Environment-Variables.md](Environment-Variables.md) — Comprehensive list of environment configuration variables.
13. [Business-Rules.md](Business-Rules.md) — Comprehensive business logic, NBA attainment calculation rules, ATR approvals.
14. [Change-Log.md](Change-Log.md) — System version history and Flyway migration tracking log.
