# Developer Setup & Local Development Guide

## Prerequisites
- **Java Development Kit**: JDK 21 LTS (`java --version`)
- **Build Tool**: Apache Maven 3.9+ (`mvn --version`)
- **Database**: PostgreSQL 16+
- **IDE**: IntelliJ IDEA 2024+ / VS Code with Spring Boot Extension Pack

## 1. Local Database Setup

Create local PostgreSQL database named `dypiu_obe_db`:
```sql
CREATE DATABASE dypiu_obe_db;
```

## 2. Environment Configuration

Copy or configure environment parameters in `application.yml` or OS environment variables:
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/dypiu_obe_db
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export JWT_SECRET=dypiu_nba_attainment_system_super_secret_jwt_key_2026_java21_spring_boot_production_key
```

## 3. Build & Run Application

To run the Spring Boot application locally:
```bash
mvn spring-boot:run
```

Flyway will automatically execute all SQL migrations (`V1` to `V6`) on start up and populate initial master data.

## 4. Health Check Verification

Open browser or cURL:
```bash
curl http://localhost:8080/api/v1/health
```
Response:
```json
{
  "success": true,
  "message": "DYPIU NBA Attainment Backend is running successfully",
  "data": {
    "status": "UP",
    "system": "DYPIU NBA Attainment System",
    "javaVersion": "21",
    "springBoot": "3.3.2",
    "database": "PostgreSQL",
    "migrationEngine": "Flyway"
  }
}
```
