# CLAUDE.md

## Project Layout

- Root: `C:/Users/ryand/Desktop/52 projects/arashbox`
- Backend: `backend/` — Spring Boot 3.4 / Java 17 / Maven
- Frontend: `frontend/` — Angular 19 / TypeScript / xterm.js / STOMP.js

## Common Commands

```bash
# Everything at once (from root, must use bash -c on Windows PowerShell)
bash -c "./dev.sh"

# Or individually:
docker-compose up -d          # Infrastructure (postgres + redis)
cd backend && ./mvnw spring-boot:run   # Backend
cd frontend && npm start               # Frontend
```

## Ports

- Frontend: http://localhost:4200
- Backend: http://localhost:9000
- PostgreSQL: localhost:5433
- Redis: localhost:6379

## Key Files

- Backend config: `backend/src/main/resources/application.yml`
- Docker client config: `backend/src/main/java/com/arashbox/config/DockerConfig.java`
- Security config: `backend/src/main/java/com/arashbox/config/SecurityConfig.java`
- WebSocket config: `backend/src/main/java/com/arashbox/config/WebSocketConfig.java`
- Code execution: `backend/src/main/java/com/arashbox/service/CodeExecutionService.java`
- WebSocket controller: `backend/src/main/java/com/arashbox/controller/ExecutionWebSocketController.java`
- Output frame DTO: `backend/src/main/java/com/arashbox/dto/OutputFrame.java`
- Main UI component: `frontend/src/app/sandbox/sandbox.component.ts`
- WebSocket service: `frontend/src/app/services/websocket.service.ts`
- API proxy: `frontend/proxy.conf.json` (proxies /api and /ws to backend)

## Platform Notes

- Windows (Docker Desktop) — Docker connects via named pipe `npipe:////./pipe/docker_engine`, NOT tcp://localhost:2375
- The backend uses `docker-java` with the `zerodep` transport (supports named pipes)
- `dev.sh` must be run as `bash -c "./dev.sh"` in PowerShell (not `.\dev.sh` or `bash .\dev.sh`)
- `dev.sh` sets JAVA_HOME automatically if missing on this machine

## Killing a Stuck Port (Windows)

```powershell
# 1. Find PID using the port
Get-NetTCPConnection -LocalPort 9000
# 2. Confirm what process it is
Get-Process -Id <PID>
# 3. Kill it
Stop-Process -Id <PID> -Force
```

## Code Execution Architecture

- Code and stdin are passed as base64-encoded env vars (`CODE_B64`, `STDIN_B64`)
- Container decodes them to `/tmp/code.{ext}` and `/tmp/stdin.txt` at startup
- This avoids the read-only rootfs issue (tmpfs on /tmp is only available after container starts)
- Streaming: `executeStreaming()` pushes `OutputFrame` messages via a `Consumer<OutputFrame>` callback
- REST `execute()` delegates to `executeStreaming()` internally

## Tools

- Do NOT use the Playwright MCP tool. Never use it for testing or browser automation.

## Coding Style Rules

- Only add comments for non-obvious business logic, complex algorithms, or when explaining "why" decisions were made
- Avoid obvious comments that just restate what the code does
- Keep code clean and self-documenting through clear naming

## Conventions

- Backend follows standard Spring Boot package structure: config/, controller/, dto/, model/, repository/, service/
- Frontend uses Angular standalone components
- All API routes prefixed with `/api`
- WebSocket endpoint at `/ws` (STOMP), messages at `/app/execute` and `/topic/execution/{sessionId}/output`
- OAuth endpoints require auth, execution is public
- Code execution runs in ephemeral Docker containers with no network, 128MB memory, 10s timeout, read-only rootfs
- Supported languages defined in `CodeExecutionService.LANGUAGE_IMAGES` map
- Track project status and roadmap in `PROJECT.md`
