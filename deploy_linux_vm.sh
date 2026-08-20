#!/usr/bin/env bash
# ==============================================================================
# DYPIU NBA Attainment System - Linux VM Automated Deployment Script
# Target Architecture:
#   Frontend: Port 3010 (React SPA served via Nginx)
#   Backend:  Port 8010 (Spring Boot Java 21)
#   Database: Port 5432 (PostgreSQL - Private/Internal)
# ==============================================================================

set -euo pipefail

echo "=========================================================="
echo " Starting DYPIU NBA Attainment Production Deployment Setup"
echo "=========================================================="

APP_USER="dypiu"
BACKEND_DIR="/opt/dypiu-nba/backend"
FRONTEND_DIR="/var/www/dypiu-nba-frontend"
CONFIG_DIR="/etc/dypiu-nba"

# 1. Create dedicated system user if not exists
if ! id -u "$APP_USER" >/dev/null 2>&1; then
    echo "[1/7] Creating system user '$APP_USER'..."
    sudo useradd -r -s /bin/false -d /opt/dypiu-nba "$APP_USER" || true
fi

# 2. Create required directories
echo "[2/7] Creating application and storage directories..."
sudo mkdir -p "$BACKEND_DIR/storage"
sudo mkdir -p "$BACKEND_DIR/uploads"
sudo mkdir -p "$FRONTEND_DIR"
sudo mkdir -p "$CONFIG_DIR"

# 3. Setup backend environment file if not already present
if [ ! -f "$CONFIG_DIR/backend.env" ]; then
    echo "[3/7] Setting up initial $CONFIG_DIR/backend.env template..."
    sudo cp ./obe-backend/.env.example "$CONFIG_DIR/backend.env"
    sudo chmod 600 "$CONFIG_DIR/backend.env"
    sudo chown root:"$APP_USER" "$CONFIG_DIR/backend.env"
    echo "  -> IMPORTANT: Review and update database credentials & JWT_SECRET in $CONFIG_DIR/backend.env"
fi

# 4. Copy backend JAR
echo "[4/7] Deploying backend JAR..."
if [ -f "./obe-backend/target/obe-backend-1.0.0.jar" ]; then
    sudo cp ./obe-backend/target/obe-backend-1.0.0.jar "$BACKEND_DIR/"
else
    echo "  -> Backend JAR not found in ./obe-backend/target. Building now..."
    cd ./obe-backend
    ./mvnw clean package -DskipTests
    sudo cp ./target/obe-backend-1.0.0.jar "$BACKEND_DIR/"
    cd ..
fi

# Set directory permissions for backend
sudo chown -R "$APP_USER:$APP_USER" "$BACKEND_DIR"
sudo chmod -R 750 "$BACKEND_DIR"

# 5. Install backend systemd service
echo "[5/7] Installing systemd service: nba-backend.service..."
sudo cp ./nba-backend.service /etc/systemd/system/
sudo systemctl daemon-reload

# 6. Deploy frontend static build to Nginx directory
echo "[6/7] Deploying frontend build..."
if [ -d "../DYPIU-NBA-Attainment-frontend-new/dist" ]; then
    sudo cp -r ../DYPIU-NBA-Attainment-frontend-new/dist/* "$FRONTEND_DIR/"
elif [ -d "./frontend-dist" ]; then
    sudo cp -r ./frontend-dist/* "$FRONTEND_DIR/"
else
    echo "  -> Frontend build directory not found. Please build frontend with 'npm run build' and copy 'dist/' contents to $FRONTEND_DIR."
fi
sudo chown -R www-data:www-data "$FRONTEND_DIR" 2>/dev/null || sudo chown -R nginx:nginx "$FRONTEND_DIR" 2>/dev/null || true
sudo chmod -R 755 "$FRONTEND_DIR"

# 7. Configure Nginx on Port 3010
echo "[7/7] Configuring Nginx on Port 3010..."
if [ -f "../DYPIU-NBA-Attainment-frontend-new/nginx.conf" ]; then
    sudo cp ../DYPIU-NBA-Attainment-frontend-new/nginx.conf /etc/nginx/conf.d/dypiu-nba.conf 2>/dev/null || \
    sudo cp ../DYPIU-NBA-Attainment-frontend-new/nginx.conf /etc/nginx/sites-available/dypiu-nba 2>/dev/null || true
    if [ -d "/etc/nginx/sites-enabled" ]; then
        sudo ln -sf /etc/nginx/sites-available/dypiu-nba /etc/nginx/sites-enabled/dypiu-nba || true
    fi
    sudo nginx -t && sudo systemctl reload nginx || true
fi

echo "=========================================================="
echo " Deployment Setup Complete!"
echo " Next Steps:"
echo " 1. Edit /etc/dypiu-nba/backend.env with your real DB password and JWT secret."
echo " 2. Start/Restart backend: sudo systemctl restart nba-backend"
echo " 3. Verify backend health: curl http://localhost:8010/api/v1/health"
echo " 4. Open frontend in browser: http://<VM-IP>:3010"
echo "=========================================================="
