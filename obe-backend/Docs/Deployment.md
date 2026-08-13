# Local Server Deployment Guide

## Target Environment Specification

The application is deployed on a **Virtual Machine inside a local server** environment at DYPIU (not cloud hosted).

- **OS**: Ubuntu 22.04 LTS / Windows Server
- **Runtime**: OpenJDK 21 LTS
- **Database**: PostgreSQL 16
- **Reverse Proxy**: Nginx 1.24

## 1. Build Production Executable Jar

```bash
mvn clean package -DskipTests
```
Generates executable JAR: `obe-backend/target/obe-backend-1.0.0.jar`.

## 2. PostgreSQL Database Setup

```sql
CREATE DATABASE dypiu_obe_db;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE dypiu_obe_db TO postgres;
```

## 3. Systemd Service Setup (`obe-backend.service`)

Create `/etc/systemd/system/obe-backend.service`:

```ini
[Unit]
Description=DYPIU NBA Attainment Backend
After=syslog.target network.target postgresql.service

[Service]
User=dypiu
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /var/www/obe-backend/obe-backend-1.0.0.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable & start service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable obe-backend
sudo systemctl start obe-backend
```

## 4. Nginx Reverse Proxy Configuration

```nginx
server {
    listen 80;
    server_name obe.dypiu.ac.in;

    location /api/v1/ {
        proxy_pass http://localhost:8080/api/v1/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
