# Application Configuration Guide

## `application.yml` Parameter Reference

```yaml
server:
  port: 8080
  servlet:
    context-path: /api/v1

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dypiu_obe_db
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate # Flyway is mandatory authority

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

app:
  jwt:
    secret: <256-bit secret key>
    expiration-ms: 86400000
  storage:
    local-dir: /var/dypiu/storage
  cors:
    allowed-origins: http://localhost:5173,http://localhost:3000
```
