# System Change Log & Release History

## [1.0.0] - 2026-08-13

### Added
- **Java 21 Spring Boot Backend Core**: Initial release of production-grade Java 21 Spring Boot backend in `obe-backend`.
- **Flyway Database Migrations**: 6 versioned SQL migration scripts (`V1` to `V6`) handling schema creation and initial master seed data for PostgreSQL.
- **Spring Security & JWT**: Implemented HMAC SHA-256 JWT provider, authentication filter, and BCrypt password encoder.
- **NBA Attainment Engine**: Direct CO, Indirect CO, Overall CO attainment calculation service matching DYPIU 80/20 NBA guidelines.
- **Role-Based Authorization**: Implemented access controls for IQAC, Director, HOD, Programme Coordinator, and Faculty.
- **Complete Documentation Hub**: 15 comprehensive markdown documentation files created in `obe-backend/Docs/`.
- **Automated Test Suite**: JUnit 5 & Spring Boot test suite verifying context loading, Flyway schema migration, and attainment calculation formulas (100% pass rate).
