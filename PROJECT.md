# Arashbox

Browser-based code execution sandbox. Write code, hit run, see output. Code runs in isolated Docker containers with real-time streaming output.

## Tech Stack

| Layer    | Tech                                          |
|----------|-----------------------------------------------|
| Frontend | Angular 19, Monaco Editor, xterm.js, STOMP.js |
| Backend  | Spring Boot 3.4, Java 17                      |
| Database | PostgreSQL 16                                 |
| Auth     | GitHub OAuth2                                 |
| Runtime  | Docker (containers per execution)             |

## Prerequisites

- Java 17+
- Node 20+
- Docker Desktop (must be running)
- PostgreSQL (via docker-compose)

## Running Locally

### Quick start

```bash
bash -c "./dev.sh"
```

This starts everything: Docker infrastructure, backend (port 9000), and frontend (port 4200).

### Manual start

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Start backend
cd backend
./mvnw spring-boot:run

# 3. Start frontend (in another terminal)
cd frontend
npm install
npm start
```

### Pull Docker images for code execution

```bash
docker pull python:3.12-slim
docker pull node:20-slim
```

### Environment variables

GitHub OAuth credentials are configured in `backend/src/main/resources/application.yml`. Set these environment variables or edit the file directly:

```
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
```

## API Endpoints

| Method | Endpoint                  | Auth     | Description              |
|--------|---------------------------|----------|--------------------------|
| POST   | `/api/execute`            | Public   | Execute code (REST)      |
| GET    | `/api/health`             | Public   | Health check             |
| GET    | `/api/snippets`           | OAuth    | List user's snippets     |
| POST   | `/api/snippets`           | OAuth    | Save a snippet           |
| PUT    | `/api/snippets/:id`       | OAuth    | Update a snippet         |
| DELETE | `/api/snippets/:id`       | OAuth    | Delete a snippet         |
| GET    | `/api/snippets/share/:id` | Public   | Get shared snippet       |

## WebSocket Protocol

Frontend connects via STOMP over WebSocket at `/ws/websocket`.

| Direction | Destination | Payload |
|---|---|---|
| Client → Server | `/app/execute` | `{ sessionId, code, language, stdin }` |
| Server → Client | `/topic/execution/{sessionId}/output` | `{ type: "stdout", data: "..." }` |
| Server → Client | `/topic/execution/{sessionId}/output` | `{ type: "stderr", data: "..." }` |
| Server → Client | `/topic/execution/{sessionId}/output` | `{ type: "exit", exitCode: 0, executionTimeMs: 123 }` |
| Server → Client | `/topic/execution/{sessionId}/output` | `{ type: "error", message: "..." }` |

## Supported Languages

- Python (python:3.12-slim)
- JavaScript (node:20-slim)

## Execution Limits

- Timeout: 10s
- Memory: 128MB
- CPU: 0.5 cores
- Network: disabled
- Output: 64KB max
- PIDs: 16
- Filesystem: read-only rootfs, writable tmpfs at /tmp (10MB)

## Architecture

```
Browser (Angular + Monaco + xterm.js)
    |
    | STOMP WebSocket /ws/websocket        (or HTTP POST /api/execute fallback)
    v
Spring Boot API
    |  - ExecutionWebSocketController       (streaming via STOMP)
    |  - ExecutionController                (REST, synchronous)
    |  - CodeExecutionService               (shared execution logic)
    v
Docker Engine (named pipe on Windows, unix socket on Linux)
    |
    |  Code + stdin passed as base64 env vars, decoded inside container
    |  Container command: sh -c 'printf ... | base64 -d > /tmp/code.py && ... < /tmp/stdin.txt'
    v
[python:3.12-slim] or [node:20-slim]
    |
    |  stdout/stderr streamed back as OutputFrame messages
    v
xterm.js terminal in browser (stderr shown in red)
```

## Project Structure

```
arashbox/
  backend/
    src/main/java/com/arashbox/
      config/         - Docker, Security, WebSocket config
      controller/     - REST + WebSocket endpoints
      dto/            - Request/response objects (ExecutionRequest, OutputFrame, etc.)
      model/          - JPA entities
      repository/     - Data access
      service/        - Business logic (CodeExecutionService)
    src/main/resources/
      application.yml - App config
  frontend/
    src/app/
      sandbox/        - Main editor + terminal component
      services/       - HTTP + WebSocket services
  docker-compose.yml  - PostgreSQL + Redis
  dev.sh              - One-command dev startup script
```

## Status / Roadmap

### Done
- [x] Monaco editor with syntax highlighting
- [x] Python and JavaScript execution via Docker
- [x] Resource limits (memory, CPU, timeout, no network, read-only rootfs)
- [x] Container cleanup after execution
- [x] GitHub OAuth2 login
- [x] Snippet CRUD (save/load/delete)
- [x] Snippet sharing via link
- [x] Ctrl+Enter to run
- [x] Language switching with default templates
- [x] WebSocket-based streaming output (STOMP)
- [x] Terminal output via xterm.js (replaces plain `<pre>` tag)
- [x] Stdin support (input()/readline work via stdin textarea)
- [x] Stderr displayed in red
- [x] Exit code display
- [x] REST fallback when WebSocket unavailable

### Planned
- [ ] More languages (Go, Rust, C++, Java, etc.)
- [ ] Execution history
- [ ] Rate limiting
- [ ] User dashboard
- [ ] Resizable split panes
