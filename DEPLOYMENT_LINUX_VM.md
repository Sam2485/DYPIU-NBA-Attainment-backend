# PRODUCTION DEPLOYMENT GUIDE — LINUX VM

## DYPIU NBA OBE & Attainment System

This guide provides complete, production-ready, step-by-step instructions for deploying the **DYPIU NBA Attainment System** (Spring Boot Backend + React Frontend + PostgreSQL) on a Linux Virtual Machine.

---

## 1. TARGET DEPLOYMENT ARCHITECTURE

```
                      REMOTE USER BROWSER
                               │
                       http://<VM-IP>:3010
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
   ┌─────────────────┐                   ┌─────────────────┐
   │    FRONTEND     │                   │     BACKEND     │
   │  Nginx Server   │  API Requests     │   Spring Boot   │
   │    Port 3010    ├──────────────────►│    Java 21      │
   │   (React SPA)   │                   │    Port 8010    │
   └─────────────────┘                   └────────┬────────┘
                                                  │
                                                  ▼ (Port 5432 Internal)
                                         ┌─────────────────┐
                                         │   PostgreSQL    │
                                         │  dypiu_obe_db   │
                                         └─────────────────┘
```

* **Frontend Port:** `3010` (React SPA served via Nginx with HTML5 pushState fallback)
* **Backend Port:** `8010` (Spring Boot Java 21 REST API under `/api/v1` context path)
* **Database Port:** `5432` (PostgreSQL - private/internal network access only)
* **Base API URL:** `http://<VM-IP>:8010/api/v1`

---

## 2. PORT & FIREWALL REQUIREMENTS

| Service | Port | Protocol | Scope | Exposure |
| :--- | :--- | :--- | :--- | :--- |
| **Frontend** | `3010` | TCP | HTTP | **Public / VM Network** (Accessible to users) |
| **Backend API** | `8010` | TCP | HTTP | **Public / VM Network** (Direct browser API access) |
| **PostgreSQL** | `5432` | TCP | PostgreSQL | **Private / Localhost only** (`127.0.0.1`) |
| **SSH** | `22` | TCP | SSH | Restricted / Admin only |

### Firewall Commands (UFW on Ubuntu/Debian)

```bash
# Allow SSH, Frontend, and Backend ports
sudo ufw allow 22/tcp comment 'SSH'
sudo ufw allow 3010/tcp comment 'DYPIU NBA Frontend'
sudo ufw allow 8010/tcp comment 'DYPIU NBA Backend'

# Explicitly ensure PostgreSQL is NOT publicly open
sudo ufw deny 5432/tcp comment 'Block public PostgreSQL'

# Enable firewall
sudo ufw enable
sudo ufw status verbose
```

---

## 3. STEP-BY-STEP PRODUCTION DEPLOYMENT

### STEP 1: Update System & Install Java 21

```bash
sudo apt update && sudo apt upgrade -y

# Install OpenJDK 21
sudo apt install -y openjdk-21-jdk openjdk-21-jre

# Verify Java version
java -version
# Expected: openjdk version "21.x.x"
```

---

### STEP 2: Install Node.js, Nginx & Utilities

```bash
# Install curl, git, and build tools
sudo apt install -y curl git ufw nginx

# Install Node.js LTS (v20 or v22)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Verify versions
node -v
npm -v
```

---

### STEP 3: Install & Configure PostgreSQL 16

```bash
# Install PostgreSQL and contrib package
sudo apt install -y postgresql postgresql-contrib

# Start and enable PostgreSQL
sudo systemctl enable postgresql
sudo systemctl start postgresql

# Create Database and User
sudo -u postgres psql << 'EOF'
CREATE DATABASE dypiu_obe_db;
CREATE USER dypiu_user WITH ENCRYPTED PASSWORD 'ChangeThisToAStrongPassword123!';
GRANT ALL PRIVILEGES ON DATABASE dypiu_obe_db TO dypiu_user;
ALTER DATABASE dypiu_obe_db OWNER TO dypiu_user;
\q
EOF
```

---

### STEP 4: Create Application User & Directories

```bash
# Create dedicated system user for running backend
sudo useradd -r -s /bin/false -d /opt/dypiu-nba dypiu || true

# Create directory hierarchy
sudo mkdir -p /opt/dypiu-nba/backend/storage
sudo mkdir -p /opt/dypiu-nba/backend/uploads
sudo mkdir -p /var/www/dypiu-nba-frontend
sudo mkdir -p /etc/dypiu-nba

# Set ownership
sudo chown -R dypiu:dypiu /opt/dypiu-nba
sudo chmod -R 750 /opt/dypiu-nba
```

---

### STEP 5: Clone & Build Backend

```bash
# Clone backend repository
cd /opt/dypiu-nba
sudo git clone <BACKEND_REPO_URL> backend-repo
cd backend-repo/obe-backend

# Build the production JAR using Maven wrapper
sudo ./mvnw clean package -DskipTests

# Copy JAR to deployment location
sudo cp target/obe-backend-1.0.0.jar /opt/dypiu-nba/backend/
sudo chown dypiu:dypiu /opt/dypiu-nba/backend/obe-backend-1.0.0.jar
```

---

### STEP 6: Configure Backend Production Environment

Create the secure environment file `/etc/dypiu-nba/backend.env`:

```bash
sudo nano /etc/dypiu-nba/backend.env
```

Add the following environment variables (replace placeholder values):

```env
# ==============================================================================
# DYPIU NBA Attainment Backend Production Configuration
# ==============================================================================

SERVER_PORT=8010
SERVER_ADDRESS=0.0.0.0

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/dypiu_obe_db
DATABASE_USERNAME=dypiu_user
DATABASE_PASSWORD=ChangeThisToAStrongPassword123!
DATABASE_DRIVER=org.postgresql.Driver
DB_POOL_SIZE=20

# Flyway & Hibernate
HIBERNATE_DDL_AUTO=validate
FLYWAY_ENABLED=true

# JWT Secret (Generate a strong key using: openssl rand -base64 48)
JWT_SECRET=c3VwZXJfc2VjdXJlX2R5cGl1X25iYV9hdHRhaW5tZW50X3Byb2R1Y3Rpb25fa2V5XzIwMjZfamF2YTIx
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000

# File Upload & Storage Locations
LOCAL_STORAGE_PATH=/opt/dypiu-nba/backend/storage
UPLOAD_STORAGE_PATH=/opt/dypiu-nba/backend/uploads

# Allowed CORS Origins (Include VM IP and/or Domain on Port 3010)
CORS_ALLOWED_ORIGINS=http://<VM-IP>:3010,http://localhost:3010
```

Secure the permissions:
```bash
sudo chmod 600 /etc/dypiu-nba/backend.env
sudo chown root:dypiu /etc/dypiu-nba/backend.env
```

---

### STEP 7: Install Backend Systemd Service

Create `/etc/systemd/system/nba-backend.service`:

```ini
[Unit]
Description=DYPIU NBA Attainment Backend (Spring Boot Java 21)
After=syslog.target network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=dypiu
Group=dypiu
WorkingDirectory=/opt/dypiu-nba/backend
EnvironmentFile=/etc/dypiu-nba/backend.env
ExecStart=/usr/bin/java -Xms512m -Xmx2048m -Dspring.profiles.active=prod -Djava.security.egd=file:/dev/./urandom -jar /opt/dypiu-nba/backend/obe-backend-1.0.0.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SuccessExitStatus=143
TimeoutStopSec=30
LimitNOFILE=65536

# Hardening
ProtectSystem=full
ProtectHome=true
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

Reload and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable nba-backend
sudo systemctl start nba-backend
```

---

### STEP 8: Clone & Build Frontend

```bash
cd /opt/dypiu-nba
sudo git clone <FRONTEND_REPO_URL> frontend-repo
cd frontend-repo

# Create production environment file with your VM IP or Domain
sudo tee .env.production << 'EOF'
VITE_API_BASE_URL=http://<VM-IP>:8010/api/v1
EOF

# Install dependencies and build
npm install
npm run build

# Deploy built static assets to web root
sudo cp -r dist/* /var/www/dypiu-nba-frontend/
sudo chown -R www-data:www-data /var/www/dypiu-nba-frontend
sudo chmod -R 755 /var/www/dypiu-nba-frontend
```

---

### STEP 9: Configure Nginx to Serve Frontend on Port 3010

Create `/etc/nginx/sites-available/dypiu-nba`:

```nginx
server {
    listen 3010;
    server_name _;

    root /var/www/dypiu-nba-frontend;
    index index.html;

    # Allow Excel and survey file uploads up to 25MB
    client_max_body_size 25M;

    # Gzip Compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied expired no-cache no-store private auth;
    gzip_types text/plain text/css application/json application/javascript application/x-javascript text/xml application/xml application/xml+rss text/javascript image/svg+xml;

    # Single Page Application (SPA) Routing for React Router (Prevents 404 on page refresh)
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(?:css|js|png|jpg|jpeg|gif|ico|svg|woff2?|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, no-transform";
    }

    # Optional proxy to backend API
    location /api/v1/ {
        proxy_pass http://127.0.0.1:8010/api/v1/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        client_max_body_size 25M;
    }

    error_page 500 502 503 504 /5x.html;
    location = /5x.html {
        root /var/www/dypiu-nba-frontend;
    }
}
```

Enable site and restart Nginx:
```bash
sudo ln -sf /etc/nginx/sites-available/dypiu-nba /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
sudo systemctl enable nginx
```

---

## 4. PROCESS MANAGEMENT COMMANDS

### Backend Management

```bash
# Check status
sudo systemctl status nba-backend

# View real-time logs
sudo journalctl -u nba-backend -f

# Start / Stop / Restart
sudo systemctl start nba-backend
sudo systemctl stop nba-backend
sudo systemctl restart nba-backend
```

### Frontend / Nginx Management

```bash
# Test Nginx configuration
sudo nginx -t

# Reload / Restart Nginx
sudo systemctl reload nginx
sudo systemctl restart nginx

# View Nginx access & error logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

---

## 5. VERIFICATION & POST-DEPLOYMENT TESTING

### 1. Verify Backend Health Endpoint

Run from terminal or browser:
```bash
curl -i http://<VM-IP>:8010/api/v1/health
```

Expected Response (`200 OK`):
```json
{
  "success": true,
  "message": "DYPIU NBA Attainment Backend is running successfully",
  "data": {
    "status": "UP",
    "system": "DYPIU NBA Attainment System",
    "javaVersion": "21.x.x",
    "springBoot": "3.3.2",
    "database": "PostgreSQL",
    "migrationEngine": "Flyway"
  }
}
```

### 2. Verify Frontend Delivery
Open `http://<VM-IP>:3010` in a browser from a separate remote machine:
* Login page renders cleanly.
* Static CSS/JS bundles load without 404s.

### 3. Verify Authentication & CORS
1. Log in with admin / faculty credentials.
2. Open Browser DevTools -> **Network Tab**:
   * Request goes to `http://<VM-IP>:8010/api/v1/auth/login`.
   * Response returns JWT token (`accessToken`).
   * Subsequent API calls include `Authorization: Bearer <token>`.
   * CORS headers are present (`Access-Control-Allow-Origin: http://<VM-IP>:3010`).

### 4. Verify React SPA Direct Route & Refresh
1. Navigate to `/director/dashboard` or `/hod/dashboard`.
2. Press `F5` / Refresh in browser.
3. Verify the page reloads the React application without a 404 Not Found error.

### 5. Verify File Upload & Excel Export
1. Upload an End-Semester marks or Survey Excel spreadsheet.
2. Verify multipart request processes without size restriction errors.
3. Download an Attainment or Action Taken Report (ATR) Excel sheet.
4. Verify binary file downloads with valid headers and data.
