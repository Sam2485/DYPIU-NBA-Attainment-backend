# Automated Testing Strategy

## Test Suite Overview

The backend maintains a comprehensive automated testing suite built with JUnit 5, Mockito, Spring Boot Test, and H2 database in PostgreSQL compatibility mode.

```text
src/test/java/com/dypiu/nba/
├── ObeBackendApplicationTests.java         # Spring Context load & Flyway migration test
└── service/
    └── AttainmentCalculationServiceTest.java # Unit tests for NBA Attainment Engine formula logic
```

## Running Tests

Execute full test suite:
```bash
mvn test -Dspring.profiles.active=test
```

### Verified Test Cases
1. **Context Loading & Flyway Schema Creation**: Ensures Spring Boot context initializes and all Flyway DDL scripts (V1 through V6) execute sequentially on a fresh database.
2. **CO Attainment Calculation Formula**: Verifies formula logic for Direct CO attainment %, Direct level (1-3), Indirect level, Overall weighted attainment (`Direct * 80% + Indirect * 20%`), and target achievement evaluation.
