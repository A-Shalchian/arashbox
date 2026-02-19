#!/bin/bash

# Start Arashbox dev environment (backend + frontend)
# Usage: bash -c "./dev.sh"

# Windows: JAVA_HOME isn't always inherited by Git Bash
if [ -z "$JAVA_HOME" ]; then
  export JAVA_HOME="C:/Program Files/Microsoft/jdk-17.0.11.9-hotspot"
fi

cleanup() {
  trap - SIGINT SIGTERM
  echo ""
  echo "Shutting down..."
  kill 0 2>/dev/null
  wait 2>/dev/null
  exit 0
}
trap cleanup SIGINT SIGTERM

echo "=== Starting infrastructure ==="
docker-compose up -d || { echo "Docker Compose failed. Is Docker Desktop running?"; exit 1; }

echo ""
echo "=== Starting backend (localhost:9000) ==="
(cd backend && cmd.exe /c "mvnw.cmd spring-boot:run") &

echo ""
echo "=== Starting frontend (localhost:4200) ==="
(cd frontend && npm start) &

echo ""
echo "Both services starting. Press Ctrl+C to stop all."
wait
