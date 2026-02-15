You are Claude Code acting as a senior full-stack architect and implementer. Build a production-ready, Dockerized application that helps insurance agents manage leads/products/calendar, run AI-initiated voice calls using OpenAI Realtime API, understand prospect needs, recommend suitable plans using a vector store, and email a personalized prospectus.

## 0) Deliverables

Produce a mono-repo with:

- **Backend:** Flask API and LangChain OpenAI APIs, plus Realtime voice integration service.
- **Frontend:** modern web UI (React + TypeScript recommended; or Angular if you prefer), with an Admin UI and Agent UI.
- **Database:** configurable DB setup (PostgreSQL default), plus a vector store (pgvector recommended, configurable).
- **Infra:** Docker Compose for local dev; Dockerfiles for frontend + backend; environment-driven configuration.
- **CI/CD:** GitHub Actions pipelines:
  - Build/test on PR
  - Build/push Docker images on main
  - Deploy via docker compose on a target VM (or provide a generic deployment job template with SSH), OR provide Kubernetes manifests as optional.
- **Docs:** README with local setup, environment variables, OpenAI API config, and how to run voice calling.

## 1) Core Personas & Roles

### Agent (standard user)
- Manage assigned leads
- View/search products and product documents
- Schedule call attempts based on prospect preferred times
- Initiate or trigger bot-initiated voice calls
- Review call transcripts, extracted needs, and recommendations
- Generate and email prospectus to prospect (capture email)

### Admin
- Manage agents/users
- Add/edit product categories, products, documents
- Configure model selection and API keys (OpenAI Realtime + OpenAI text)
- Configure DB and vector store settings (via env + UI where appropriate)
- Audit logs & monitoring

## 2) Functional Requirements (Must Implement)

### A) Lead Management
Leads are assigned to agents.

**Lead fields:**
- Name, phone, email (optional initially), location, age (optional), income band (optional), notes
- Lead source, status (NEW/CONTACTED/QUALIFIED/PROPOSAL_SENT/CONVERTED/LOST)
- Preferred contact time windows (day/time ranges + timezone)
- Consent flags (DND/opt-out)

**Lead pipeline views:**
- Kanban by status
- List with filters/search
- Lead detail page with timeline (calls, notes, emails, proposals)

**Import leads via CSV** (admin or agent depending on permissions).

### B) Product + Product Category + Document Management
Products must be agent-accessible and admin-manageable.

Admin can:
- Add/edit product categories
- Add/edit product details
- Upload/attach documents (PDF/HTML) and show category-wise

**Product fields:**
- Name, category, insurer, plan type (term/health/ULIP/etc), eligibility, coverage highlights
- Premium ranges, exclusions, riders, claim process highlights
- Tags for retrieval, structured features (JSON)
- Document attachments

All product content must be embedded into vector store for retrieval:
- Product metadata + key features + extracted document text

Provide re-index function and scheduled indexing.

### C) Calendar / Dial Scheduling
Agents need a dial calendar:
- Show leads to call in slots based on their preferred time windows.
- Allow agent to mark call attempt outcomes.

Provide internal scheduling:
- “Call tasks” created for each lead and time window
- Automatic reminders/queue for next best call time

Optional integration: Google Calendar (keep optional; design interface but default to internal calendar).

### D) Voice Bot (OpenAI Realtime API) – AI Initiates Calls
The voice call must be initiated by a bot (the “sales agent bot”).

The bot must:
- Ask questions to identify customer needs (coverage needs, budget, family, existing policies, health, goals, risk tolerance).
- Be empathetic and not pushy (explicitly enforce tone).
- Handle objections politely.
- Ask for permission to email details and collect email if missing.

**Conversation outputs:**
- Full transcript
- Extracted structured “Needs Profile”
- Recommended products (with reasoning)
- Prospectus draft

**Implementation requirement:**
- Use OpenAI Realtime API for voice conversations.
- Implement a voice session orchestrator service:
  - Start call -> connect Realtime session -> stream audio -> receive transcript/events
  - Persist transcript and extracted needs in DB
- If actual PSTN calling is needed, integrate via Twilio/Plivo (choose one) to bridge phone audio to the Realtime session.
- Even if you can’t fully run PSTN locally, implement the architecture and provide a mock mode for local dev.

### E) Product Recommendation via Vector Store
Based on conversation and needs profile:
- Retrieve relevant products using similarity search + metadata filters (category, budget, age eligibility).
- Present top 3–5 products with explainable rationale and key benefits.

Use a RAG pipeline:
- Query = summarized needs + constraints
- Retrieve product chunks
- Use LLM to rank and generate recommendation narrative

Must avoid hallucinating product details:
- Only mention details present in DB/docs; cite product sources internally.

### F) Prospectus Generation + Email

**Prospectus:**
- A structured PDF (or HTML->PDF) personalized to the prospect
- Includes: needs summary, recommended products comparison table, next steps, FAQ
- Tone: helpful, non-pushy, compliant-friendly

**Email workflow:**
- Capture email during call or afterwards
- Send email with prospectus attached + brief personalized summary
- Use SMTP (configurable) or SendGrid (optional)
- Store sent emails and status.

### G) Admin UI: Model Selection + API Keys
Admin can configure:
- OpenAI API keys (Realtime + standard text)
- Model selection for:
  - Voice Realtime model (e.g., realtime voice)
  - Text LLM (for extraction, recommendation, prospectus)
  - Embedding model for vector store

Store secrets securely:
- In production: prefer environment variables / secret manager
- In-app storage must encrypt at rest (if you store keys in DB) and restrict access

Provide audit log for changes.

### H) Configurable DB Configurations
DB connection strings/config must be environment-driven:
- Postgres default, but support MySQL as optional profile

Vector store config also environment-driven:
- pgvector default; optional Pinecone/Qdrant via interface-based design

Support different environments: dev, test, prod.

## 3) Non-Functional Requirements

### Security
- Auth: JWT-based login
- RBAC: ADMIN vs AGENT
- Data access: agents only see assigned leads; admin sees all
- Input validation + rate limits for critical endpoints

### Observability
- Structured logging
- Basic metrics endpoint (Micrometer + Prometheus format)

### Compliance
- Store consent flags, call recording/transcript permissions
- Ensure email opt-out handling

### Performance
- Async processing for embeddings/prospectus generation
- Pagination for lists

### Testing
- Backend unit + integration tests
- Frontend component smoke tests
- CI runs tests

## 4) System Architecture (Implement This)

Use a modular approach:

### Backend (Fast API) modules/packages
- auth: login, JWT, RBAC
- leads: CRUD + assignment + timeline
- products: categories/products/docs + indexing
- scheduler: call tasks, preferred-time matching
- voice: Realtime session orchestration, PSTN bridge adapter, transcript storage
- rag: embeddings, vector search, recommendation ranking
- prospectus: generate HTML + PDF, store versioned outputs
- email: SMTP/SendGrid adapter + email log
- admin: model/key config + audit logs
- common: DTOs, errors, config, utilities

### Frontend
Two apps or one app with role-based routing:
- /agent/* for agents
- /admin/* for admin

**Pages:**
- Login
- Agent Dashboard: today’s call queue + calendar + lead pipeline
- Leads: list/kanban + lead detail timeline
- Products: category-wise documents + product detail
- Calls: session history, transcript, extracted needs, recommendations
- Prospectus: preview + send email
- Admin: users/agents, product management, model config, API keys, DB/vector config display, audit logs

## 5) Data Model (Minimum Tables)

Implement migrations using Flyway or Liquibase.

### Users
- id, name, email, hashed_password, role, created_at

### Leads
- id, assigned_agent_id, name, phone, email, status, preferred_time_windows (json), timezone, consent_flags (json), created_at, updated_at

### LeadActivities
- id, lead_id, type (NOTE/CALL/EMAIL/STATUS_CHANGE), payload (json), created_at

### ProductCategories
- id, name, description

### Products
- id, category_id, name, insurer, details_json, eligibility_json, tags, created_at, updated_at

### ProductDocuments
- id, product_id, category_id, filename, storage_url/path, extracted_text, created_at

### VectorEmbeddings
- id, entity_type (PRODUCT/DOC_CHUNK), entity_id, chunk_text, embedding (pgvector), metadata_json

### CallTasks
- id, lead_id, agent_id, scheduled_time, status (PENDING/DONE/MISSED), created_at

### VoiceSessions
- id, lead_id, agent_id, started_at, ended_at, transcript_text, extracted_needs_json, recommendations_json, status

### Prospectus
- id, lead_id, agent_id, version, html_content, pdf_path, created_at

### EmailLogs
- id, lead_id, agent_id, to_email, subject, status, provider_message_id, created_at

### AdminSettings
- id, key, encrypted_value, updated_by, updated_at

### AuditLogs
- id, actor_id, action, entity, entity_id, before_json, after_json, created_at

## 6) AI/LLM Design Requirements

### Needs Extraction (Text LLM)
After call ends (or during), run extraction to produce:
- coverage_type, budget_range, dependents, goals, existing_policy, urgency, objections, health_notes (if provided), preferred_contact_mode

Output must be strict JSON schema; validate with a JSON parser.

### Retrieval + Recommendation
Build RAG:
- Embed product docs and product summaries
- Retrieve top K chunks
- Rank and generate recommendation summary
- Must cite product IDs used in the recommendation payload.

### Prospectus
Generate:
- Needs summary
- Product comparison table
- Next steps + disclaimers

Produce HTML then generate PDF via a suitable library backend service.

### Empathetic Voice Agent Prompting
The Realtime voice agent must follow rules:
- polite, empathetic, not pushy
- asks permission before sensitive questions
- respects “not interested” and offers to follow up later
- collects email gently

## 7) OpenAI Integration Requirements

### Realtime Voice
Create VoiceBotService that:
- Initiates a session with OpenAI Realtime API
- Handles streaming events, transcripts, and tool calls

Implement “tools” (function calling) that the voice bot can invoke:
- lookupProducts(needsJson) -> returns candidate products
- saveEmail(leadId, email) -> persist email
- scheduleFollowUp(leadId, datetime) -> create call task

Log the entire event stream (redacted) for debugging.

### Text LLM (OpenAI APIs via Langchain)
Use Langchain abstraction for chat + embeddings.

Model selection is configurable by Admin settings and/or env.

## 8) Storage & File Handling
Store uploaded product documents:
- Local in dev
- S3-compatible storage in prod (optional, pluggable)

Extract text from PDFs for indexing (use Apache Tika).

Keep original file downloadable in UI by category.

## 9) API Design (Backend)
Provide REST endpoints (minimum):
- /api/auth/login
- /api/leads CRUD + /api/leads/{id}/activities
- /api/products/categories, /api/products, /api/products/{id}/documents
- /api/scheduler/tasks (create/complete/list)
- /api/voice/sessions (start, stop, get transcript, get needs, get recommendations)
- /api/prospectus/{leadId} (generate, list versions, download pdf)
- /api/email/send (send prospectus)
- /api/admin/settings (models, api keys, config) + /api/admin/audit

## 10) Frontend Requirements
Use a component library (e.g., MUI, Chakra, or AntD).

Must include:
- Category-wise document browsing
- Lead call queue/calendar view
- Voice session history with transcript viewer + extracted needs + recommended products
- Prospectus preview (HTML view) and download
- Admin settings forms (model dropdowns, API key management)

Use environment-based API URL configuration.

Implement token storage securely (httpOnly cookies preferred; if not, localStorage with care).

## 11) Docker Convention
Provide:
- backend/Dockerfile
- frontend/Dockerfile
- docker-compose.yml with services:
  - postgres (with pgvector)
  - backend
  - frontend

Include .env.example and document required env vars.

## 12) GitHub Actions
Implement workflows:

### ci.yml
- run backend tests (mvn test)
- run frontend tests/build
- build docker images

### cd.yml
- on push to main: build & push images to GHCR
- deploy:
  - Option A: SSH into VM, pull images, restart docker compose

Provide secrets list needed: SSH_HOST, SSH_USER, SSH_KEY, etc.

## 13) Local Dev Experience
make up or ./run-dev.sh that starts everything.

Seed data:
- Admin user
- Sample agent user
- Sample products + categories
- Sample lead

Provide mock voice mode if PSTN is not configured:
- Simulate a voice session via text input and still produce transcript/needs/recommendation/prospectus

## 14) Acceptance Criteria Checklist
Your output is complete only if:
- Agents can manage leads, view assigned leads only
- Admin can add products, categories, documents; documents shown category-wise
- Dial scheduler queues leads based on preferred time windows
- Voice bot can run a “conversation” and store transcript + extracted needs
- RAG retrieves from vector store and suggests products
- Prospectus generated and emailed
- Admin UI supports model selection and API keys
- DB/vector configs are environment-driven and documented
- Docker + docker compose run locally
- GitHub Actions pipelines exist and pass

## 15) Implementation Notes / Constraints
- Use clean architecture principles.
- Use DTOs and mapstruct (optional).
- Handle errors with consistent JSON error responses.
- Add basic OpenAPI/Swagger docs.
- Do not hardcode API keys; read from env or admin settings store.
- Ensure secrets are not logged.

Now generate the full repository with code, configs, Docker, workflows, and README. Also include a short “Architecture Overview” section in README.
