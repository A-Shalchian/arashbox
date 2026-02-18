# CLAUDE.md

## Project Layout

- Root: `C:/Users/ryand/Desktop/52 projects/arashbox`
- Backend: `backend/` — Spring Boot 3.4 / Java 17 / Maven
- Frontend: `frontend/` — Angular 19 / TypeScript

## Common Commands

```bash
# Everything at once (from root)
./dev.sh

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
- Code execution: `backend/src/main/java/com/arashbox/service/CodeExecutionService.java`
- Main UI component: `frontend/src/app/sandbox/sandbox.component.ts`
- API proxy: `frontend/proxy.conf.json` (proxies /api and /ws to backend)

## Platform Notes

- Windows (Docker Desktop) — Docker connects via named pipe `npipe:////./pipe/docker_engine`, NOT tcp://localhost:2375
- The backend uses `docker-java` with the `zerodep` transport (supports named pipes)

## Coding Style Rules

- Only add comments for non-obvious business logic, complex algorithms, or when explaining "why" decisions were made
- Avoid obvious comments that just restate what the code does
- Keep code clean and self-documenting through clear naming

## Conventions

- Backend follows standard Spring Boot package structure: config/, controller/, dto/, model/, repository/, service/
- Frontend uses Angular standalone components
- All API routes prefixed with `/api`
- OAuth endpoints require auth, execution is public
- Code execution runs in ephemeral Docker containers with no network, 128MB memory, 10s timeout
- Supported languages defined in `CodeExecutionService.LANGUAGE_IMAGES` map
- Track project status and roadmap in `PROJECT.md`