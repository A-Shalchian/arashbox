#!/bin/bash

# Start Arashbox dev environment (backend + frontend)
# Usage: ./dev.sh

trap 'echo "Shutting down..."; kill 0; exit' SIGINT SIGTERM

echo "=== Starting infrastructure ==="
docker-compose up -d || { echo "Docker Compose failed. Is Docker Desktop running?"; exit 1; }

echo ""
echo "=== Starting backend (localhost:9000) ==="
(cd backend && ./mvnw spring-boot:run) &

echo ""
echo "=== Starting frontend (localhost:4200) ==="
(cd frontend && npm start) &

echo ""
echo "Both services starting. Press Ctrl+C to stop all."
wait
