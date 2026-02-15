# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Insurance Agent Outreach + Voice AI Prospectus App - A production-ready application combining CRM, AI voice calling (OpenAI Realtime API), RAG-based product recommendations (pgvector), and automated prospectus generation.

**Tech Stack:**
- Backend: Java 21 + Spring Boot 3.2.2 + Spring AI
- Frontend: React 18 + TypeScript + Vite + Material-UI
- Database: PostgreSQL with pgvector extension
- Infrastructure: Docker + Docker Compose
- CI/CD: GitHub Actions

## Development Commands

### Quick Start
```bash
# Start all services (requires Docker)
./run-dev.sh

# OR manually
docker-compose up --build
```

### Backend (Spring Boot)
```bash
cd backend

# Compile
mvn clean compile

# Run tests
mvn test

# Run a single test class
mvn test -pl backend -Dtest=ClassName

# Package
mvn package

# Run locally (requires PostgreSQL)
mvn spring-boot:run
```

**Note:** Local compilation may fail with Lombok + Java 21.0.5 (see `KNOWN_ISSUES.md`). Workarounds: use Docker build, Java 17, or a different JDK distribution (e.g., Temurin, Corretto).

### Frontend (React + Vite)
```bash
cd frontend

# Install dependencies
npm install

# Start dev server (port 3000, proxies /api to :8080)
npm run dev

# Build for production
npm run build

# Run linter
npm run lint

# Run tests (Vitest)
npm test
```

### Database Migrations
- Location: `backend/src/main/resources/db/migration/`
- Managed by Flyway (runs automatically on startup)
- Current latest: `V11__fix_user_passwords.sql`
- Create new: `V{next}__description.sql` (next is V12)

### Docker Commands
```bash
# Build images
docker-compose build

# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Rebuild specific service
docker-compose up --build backend
```

## Architecture

### Backend Module Structure

```
backend/src/main/java/com/insurance/
├── auth/              # JWT authentication, Spring Security config, login/register
├── leads/             # Lead CRUD, CSV import, RBAC (agents see assigned only)
├── products/          # Product catalog, categories, document upload/storage
├── scheduler/         # Call task scheduling based on preferred time windows
├── voice/             # OpenAI Realtime API, Twilio PSTN bridge, transcript storage
├── rag/               # Vector embeddings, pgvector similarity search, RAG pipeline
├── prospectus/        # HTML/PDF generation with OpenHTMLtoPDF
├── email/             # SMTP email sending, logging
├── admin/             # Admin settings, API key management, audit logs, user CRUD
└── common/entity/     # JPA entities (User, Lead, Product, VoiceSession, etc.)
```

**Key Technologies:**
- Spring AI for OpenAI chat/embeddings
- pgvector for vector similarity search
- Apache Tika for document text extraction
- OpenHTMLtoPDF for PDF generation
- Twilio for PSTN voice bridging
- JWT with Spring Security
- Flyway for migrations
- Lombok for boilerplate reduction + MapStruct for DTO mapping

### Frontend Structure

```
frontend/src/
├── api/               # Axios API clients (auth, leads, products, voice, etc.)
├── components/        # Layout, Navbar, Sidebar, PrivateRoute, LoadingSpinner
├── pages/
│   ├── agent/        # Dashboard, LeadList, LeadKanban, CallCalendar, ProductBrowser
│   └── admin/        # UserManagement, ProductManagement, ModelConfig, AuditLogs
├── store/            # Zustand auth store (user, token, login/logout)
├── types/            # TypeScript interfaces matching backend DTOs
└── utils/            # Formatters, validators
```

**Key Technologies:**
- React Router v6 for navigation
- Material-UI components
- TanStack Query for data fetching
- Zustand for state management
- react-hook-form for forms
- react-beautiful-dnd for Kanban

### Database Schema

**Core Tables:**
- `users` - Agents and admins (role: ADMIN/AGENT)
- `leads` - Lead management with JSONB for time windows/consent
- `lead_activities` - Timeline (NOTE/CALL/EMAIL/STATUS_CHANGE)
- `product_categories` - Product categorization
- `products` - Insurance products with JSONB details/eligibility
- `product_documents` - Document storage with extracted text
- `vector_embeddings` - pgvector(1536) for RAG
- `call_tasks` - Scheduled calls with preferred time matching
- `voice_sessions` - Transcripts, needs, recommendations
- `prospectus` - HTML/PDF versions
- `email_logs` - Email tracking
- `admin_settings` - Encrypted settings (API keys)
- `audit_logs` - Complete audit trail

### API Endpoints (70+ total)

**Auth:** `/api/auth/login`, `/api/auth/register`, `/api/auth/me`

**Leads:** `/api/leads` (CRUD, search, import CSV, activities)

**Products:** `/api/products`, `/api/products/categories`, `/api/products/{id}/documents`

**Voice:** `/api/voice/sessions` (start, stop, transcript, needs, recommendations)

**Scheduler:** `/api/scheduler/tasks` (CRUD, by agent, by date range)

**RAG:** `/api/rag/recommend`, `/api/rag/reindex`, `/api/rag/search`

**Prospectus:** `/api/prospectus/{leadId}` (generate, download PDF, preview HTML)

**Email:** `/api/email/send` (prospectus email)

**Admin:** `/api/admin/users`, `/api/admin/settings`, `/api/admin/audit`

Swagger UI is at `http://localhost:8080/swagger-ui.html` (disabled by default; enable with `springdoc.swagger-ui.enabled=true` in `application.yml`). See `backend/API_ENDPOINTS.md` for a full endpoint reference.

## Environment Variables

**Required:**
```bash
OPENAI_API_KEY=sk-your-key-here
JWT_SECRET=min-256-bits-secret
DB_PASSWORD=secure-password
```

**Optional (with defaults):**
```bash
TWILIO_ACCOUNT_SID=ACxxx
TWILIO_AUTH_TOKEN=xxx
SMTP_USERNAME=user@gmail.com
SMTP_PASSWORD=app-password
VOICE_MOCK_MODE=true  # Set false for real PSTN calls
```

See `.env.example` for complete list.

## Important Notes

### Security & RBAC
- Agents can ONLY see their assigned leads (enforced in repositories and controllers)
- Admins have full access
- Use `@PreAuthorize("hasRole('ADMIN')")` for admin-only endpoints
- JWT tokens stored in localStorage (frontend)
- All sensitive settings encrypted in database

### Voice AI Integration
- OpenAI Realtime API via WebSocket for voice conversations
- Twilio bridges PSTN to Realtime API
- Mock mode available for development (no API costs)
- Transcripts stored in real-time
- Needs extracted via LLM after call ends

### RAG Pipeline
1. Products/documents indexed with text-embedding-3-small
2. Chunked at 1000 chars (sentence boundaries)
3. Stored in pgvector with metadata
4. Similarity search retrieves top K chunks
5. LLM ranks and generates recommendation narrative
6. Must cite product IDs to prevent hallucination

### File Storage
- Local: `./storage` (configurable via `STORAGE_LOCAL_PATH`)
- S3-compatible storage supported (pluggable)
- Documents extracted with Apache Tika
- PDFs stored in prospectus module

### Default Credentials
```
Admin: admin@insurance.com / Admin@123
Agent: agent@insurance.com / Agent@123
```

### CI/CD Workflows
- **ci.yml**: Build/test on PRs (backend tests, frontend build, Docker build)
- **cd.yml**: Push images to GHCR on main branch
- **deploy.yml**: SSH deploy to VM with docker-compose

### Common Development Tasks

**Add new entity:**
1. Create JPA entity in `common/entity/`
2. Create repository in module `repository/`
3. Create DTOs in module `dto/`
4. Add service logic in module `service/`
5. Create controller endpoints in module `controller/`

**Add new migration:**
1. Create `V{next}__{description}.sql` in `resources/db/migration/`
2. Restart application

**Add new frontend page:**
1. Create component in `pages/agent/` or `pages/admin/`
2. Add API client method in `api/`
3. Add route in `App.tsx`
4. Add navigation in `Sidebar.tsx`

**Testing locally:**
```bash
# Backend with mock voice mode
VOICE_MOCK_MODE=true mvn spring-boot:run

# Frontend with local backend
npm run dev
```

## Troubleshooting

**Database connection failed:**
```bash
docker-compose restart postgres
docker-compose logs postgres
```

**Frontend can't connect to backend:**
- Check `VITE_API_URL` in `.env`
- Verify backend health: `curl http://localhost:8080/actuator/health`

**OpenAI API errors:**
- Verify `OPENAI_API_KEY` is valid
- Check model names match: `gpt-4o-realtime-preview-2024-12-17`, `gpt-4o`, `text-embedding-3-small`

**Vector search not working:**
- Ensure pgvector extension installed: `CREATE EXTENSION IF NOT EXISTS vector;`
- Check embeddings exist: `SELECT COUNT(*) FROM vector_embeddings;`
- Reindex products: `POST /api/rag/reindex`

## Documentation

- **README.md** - Complete setup and deployment guide
- **API_ENDPOINTS.md** - Full API reference (70+ endpoints)
- **KNOWN_ISSUES.md** - Lombok/Java 21 compatibility and workarounds
- **IMPLEMENTATION_SUMMARY.md** - Detailed feature implementation notes
- **Backend docs** - `backend/DEVELOPER_GUIDE.md`, module-specific docs
- **Frontend docs** - `frontend/README.md`

## Project Status

- All core features implemented and wired end-to-end
- Production-ready with Docker (multi-stage builds, health checks)
- CI/CD pipelines configured (GitHub Actions)
- Test infrastructure set up (Maven Surefire / Vitest) but test coverage is minimal
