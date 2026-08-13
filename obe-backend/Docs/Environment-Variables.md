# Environment Variables Specification

The following environment variables control server configuration in development and production environments.

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | HTTP port for Spring Boot Tomcat server. |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/dypiu_obe_db` | PostgreSQL JDBC connection URL. |
| `DATABASE_USERNAME` | `postgres` | PostgreSQL database user username. |
| `DATABASE_PASSWORD` | `postgres` | PostgreSQL database user password. |
| `JWT_SECRET` | *(256-bit default string)* | Secret key used to sign and verify JWT tokens. |
| `LOCAL_STORAGE_PATH` | `/var/dypiu/storage` | Disk path for storing uploaded marksheets and surveys. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated list of allowed CORS origins. |
