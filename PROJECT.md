# Arashbox

Browser-based code execution sandbox. Write code, hit run, see output. Code runs in isolated Docker containers.

## Tech Stack

| Layer    | Tech                                  |
|----------|---------------------------------------|
| Frontend | Angular 19, Monaco Editor, xterm.js   |
| Backend  | Spring Boot 3.4, Java 17              |
| Database | PostgreSQL 16                         |
| Auth     | GitHub OAuth2                         |
| Runtime  | Docker (containers per execution)     |

## Prerequisites

- Java 17+
- Node 20+
- Docker Desktop (must be running)
- PostgreSQL (via docker-compose)

## Running Locally

### 1. Start infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL on port **5433** and Redis on port **6379**.

### 2. Set environment variables

Copy `.env.example` to `.env` and fill in your GitHub OAuth credentials:

```
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
```

### 3. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

Runs on **http://localhost:9000**

### 4. Start the frontend

```bash
cd frontend
npm install
npm start
```

Runs on **http://localhost:4200** (proxies `/api` and `/ws` to backend)

### 5. Pull Docker images for code execution

```bash
docker pull python:3.12-slim
docker pull node:20-slim
```

## API Endpoints

| Method | Endpoint                  | Auth     | Description              |
|--------|---------------------------|----------|--------------------------|
| POST   | `/api/execute`            | Public   | Execute code             |
| GET    | `/api/health`             | Public   | Health check             |
| GET    | `/api/snippets`           | OAuth    | List user's snippets     |
| POST   | `/api/snippets`           | OAuth    | Save a snippet           |
| PUT    | `/api/snippets/:id`       | OAuth    | Update a snippet         |
| DELETE | `/api/snippets/:id`       | OAuth    | Delete a snippet         |
| GET    | `/api/snippets/share/:id` | Public   | Get shared snippet       |

## Supported Languages

- Python (python:3.12-slim)
- JavaScript (node:20-slim)

## Execution Limits

- Timeout: 10s
- Memory: 128MB
- CPU: 0.5 cores
- Network: disabled

## Architecture

```
Browser (Angular + Monaco)
    |
    | HTTP POST /api/execute
    v
Spring Boot API
    |
    | Docker API (named pipe on Windows, unix socket on Linux)
    v
Docker Engine
    |
    | Creates ephemeral container
    v
[python:3.12-slim] or [node:20-slim]
    |
    | stdout/stderr captured
    v
Response returned to browser
```

## Project Structure

```
arashbox/
  backend/
    src/main/java/com/arashbox/
      config/         - Docker, Security, WebSocket config
      controller/     - REST endpoints
      dto/            - Request/response objects
      model/          - JPA entities
      repository/     - Data access
      service/        - Business logic
    src/main/resources/
      application.yml - App config
  frontend/
    src/app/
      sandbox/        - Main editor + output component
      services/       - HTTP services
  docker-compose.yml  - PostgreSQL + Redis
```

## Status / Roadmap

### Done
- [x] Monaco editor with syntax highlighting
- [x] Python and JavaScript execution via Docker
- [x] Resource limits (memory, CPU, timeout, no network)
- [x] Container cleanup after execution
- [x] GitHub OAuth2 login
- [x] Snippet CRUD (save/load/delete)
- [x] Snippet sharing via link
- [x] Ctrl+Enter to run
- [x] Language switching with default templates

### Planned
- [ ] WebSocket-based streaming output
- [ ] More languages (Go, Rust, C++, Java, etc.)
- [ ] Terminal output via xterm.js
- [ ] Execution history
- [ ] Rate limiting
- [ ] User dashboard
