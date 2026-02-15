# Insurance Agent Outreach + Voice AI Prospectus App

A production-ready, full-stack application that helps insurance agents manage leads, run AI-initiated voice calls using OpenAI Realtime API, understand prospect needs, recommend suitable plans using RAG (Retrieval-Augmented Generation), and email personalized prospectuses.

## 🎯 Overview

This application streamlines the insurance sales process by combining traditional CRM features with cutting-edge AI voice technology and intelligent product recommendations.

### Key Features

- **Lead Management**: Comprehensive CRM for managing insurance leads with status tracking and timeline
- **AI Voice Calls**: Automated voice conversations using OpenAI Realtime API with PSTN integration via Twilio
- **Smart Scheduling**: Intelligent call scheduling based on prospect preferred time windows
- **RAG-Powered Recommendations**: Vector-based product search with AI-generated recommendations
- **Prospectus Generation**: Automated creation of personalized PDF prospectuses
- **Email Automation**: Smart email delivery with prospectus attachments
- **Product Management**: Comprehensive product catalog with category-wise document organization
- **Admin Dashboard**: System configuration, user management, and audit logging

## 🏗️ Architecture Overview

### Technology Stack

**Backend:**
- Java 21 + Spring Boot 3.2.2
- Spring AI for OpenAI integration
- PostgreSQL with pgvector for vector search
- Flyway for database migrations
- JWT authentication with Spring Security
- OpenHTMLtoPDF for PDF generation
- Apache Tika for document text extraction
- Twilio for PSTN voice calls

**Frontend:**
- React 18 + TypeScript
- Vite for build tooling
- Material-UI (MUI) for components
- Zustand for state management
- TanStack Query for data fetching
- React Router v6 for navigation

**Infrastructure:**
- Docker & Docker Compose for containerization
- GitHub Actions for CI/CD
- PostgreSQL with pgvector extension

### System Architecture

```
┌─────────────────┐         ┌─────────────────┐         ┌──────────────────┐
│                 │         │                 │         │                  │
│  React Frontend │────────▶│  Spring Boot    │────────▶│  PostgreSQL +    │
│  (Port 3000)    │         │  Backend        │         │  pgvector        │
│                 │         │  (Port 8080)    │         │  (Port 5432)     │
└─────────────────┘         └─────────────────┘         └──────────────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
              ┌─────▼─────┐   ┌─────▼─────┐   ┌──────▼──────┐
              │  OpenAI   │   │  Twilio   │   │  SMTP/      │
              │  APIs     │   │  (PSTN)   │   │  SendGrid   │
              └───────────┘   └───────────┘   └─────────────┘
```

## 📦 Module Structure

### Backend Modules

```
backend/src/main/java/com/insurance/
├── auth/              # JWT authentication & authorization
├── leads/             # Lead management with RBAC
├── products/          # Product catalog & document storage
├── scheduler/         # Call task scheduling
├── voice/             # OpenAI Realtime API & Twilio integration
├── rag/               # Vector search & recommendations
├── prospectus/        # PDF generation
├── email/             # Email sending & logging
├── admin/             # Admin settings & audit logs
└── common/            # Shared entities & utilities
```

### Frontend Structure

```
frontend/src/
├── api/               # API client layer
├── components/        # Reusable UI components
├── pages/
│   ├── agent/        # Agent UI pages
│   └── admin/        # Admin UI pages
├── store/            # Zustand state management
├── types/            # TypeScript interfaces
└── utils/            # Helper functions
```

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Git
- (Optional) Java 21 & Maven for local backend development
- (Optional) Node.js 20+ for local frontend development

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd insurance-app
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your actual credentials
   ```

3. **Required Environment Variables**

   Minimum required variables:
   ```bash
   OPENAI_API_KEY=sk-your-openai-api-key-here
   JWT_SECRET=your-jwt-secret-key-min-256-bits
   DB_PASSWORD=your-secure-password
   ```

   Optional variables (can use defaults):
   ```bash
   TWILIO_ACCOUNT_SID=your_twilio_sid
   TWILIO_AUTH_TOKEN=your_twilio_token
   SMTP_USERNAME=your-email@gmail.com
   SMTP_PASSWORD=your-app-password
   ```

4. **Start the application**
   ```bash
   chmod +x run-dev.sh
   ./run-dev.sh
   ```

   This script will:
   - Start PostgreSQL with pgvector
   - Build and start the backend
   - Build and start the frontend
   - Run database migrations
   - Seed initial data

5. **Access the application**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - API Documentation: http://localhost:8080/swagger-ui.html

### Default Credentials

```
Admin:  admin@insurance.com / Admin@123
Agent:  agent@insurance.com / Agent@123
```

## 📚 API Configuration

### OpenAI Configuration

The application uses multiple OpenAI models:

1. **Realtime Voice API** (for voice conversations)
   - Model: `gpt-4o-realtime-preview-2024-12-17`
   - Used for: AI voice agent conversations

2. **Text Generation API** (for needs extraction, recommendations)
   - Model: `gpt-4o` (configurable)
   - Used for: Needs extraction, product recommendations, prospectus content

3. **Embeddings API** (for vector search)
   - Model: `text-embedding-3-small` (configurable)
   - Used for: Product and document embeddings

Configure via Admin UI or environment variables:
```bash
OPENAI_API_KEY=sk-your-key
OPENAI_REALTIME_MODEL=gpt-4o-realtime-preview-2024-12-17
OPENAI_TEXT_MODEL=gpt-4o
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
```

### Twilio Configuration (for PSTN Calls)

```bash
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_PHONE_NUMBER=+1234567890
```

For local development without Twilio:
```bash
VOICE_MOCK_MODE=true
```

### Email Configuration

**Option 1: SMTP (Gmail)**
```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_FROM=noreply@insurance-app.com
```

**Option 2: SendGrid**
```bash
SENDGRID_API_KEY=SG.xxxxxxxxxxxxxx
```

## 🔧 Development

### Backend Development

```bash
cd backend

# Compile
mvn clean compile

# Run tests
mvn test

# Run locally (requires PostgreSQL running)
mvn spring-boot:run

# Package
mvn package
```

### Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Start dev server (with hot reload)
npm run dev

# Build for production
npm run build

# Run linter
npm run lint
```

### Database Migrations

Migrations are managed with Flyway and run automatically on application startup.

Location: `backend/src/main/resources/db/migration/`

To create a new migration:
1. Create file: `V{version}__description.sql`
2. Add SQL statements
3. Restart application

## 📊 Core Workflows

### 1. Lead to Customer Journey

```
1. Lead Import/Creation
   ↓
2. Agent Assignment
   ↓
3. Call Scheduling (based on preferred time)
   ↓
4. AI Voice Call (OpenAI Realtime API)
   ↓
5. Needs Extraction (AI-powered)
   ↓
6. Product Recommendations (RAG)
   ↓
7. Prospectus Generation (PDF)
   ↓
8. Email Delivery
   ↓
9. Follow-up & Conversion
```

### 2. Voice Call Flow

```
Agent initiates call
   ↓
Twilio connects to prospect
   ↓
Audio bridged to OpenAI Realtime API
   ↓
AI conducts conversation
   ↓
Transcript saved in real-time
   ↓
Needs extracted via LLM
   ↓
Products recommended via RAG
   ↓
Email permission requested
   ↓
Call summary logged
```

### 3. Product Recommendation (RAG)

```
Voice conversation
   ↓
Extract needs (coverage type, budget, family size, etc.)
   ↓
Generate embedding for needs
   ↓
Vector similarity search in pgvector
   ↓
Retrieve top 5-10 relevant products
   ↓
LLM ranks and generates narrative
   ↓
Return recommendations with reasoning
```

## 🐳 Docker Deployment

### Build Images

```bash
# Backend
docker build -t insurance-backend ./backend

# Frontend
docker build -t insurance-frontend \
  --build-arg VITE_API_URL=https://api.yourserver.com \
  ./frontend
```

### Run with Docker Compose

```bash
docker-compose up -d
```

Services:
- `postgres`: PostgreSQL with pgvector
- `backend`: Spring Boot application
- `frontend`: Nginx serving React app

## 🚢 Production Deployment

### Option 1: VM Deployment with Docker Compose

1. **Set up VM** (Ubuntu 22.04+ recommended)
   ```bash
   # Install Docker
   sudo apt update
   sudo apt install docker.io docker-compose-v2 -y
   ```

2. **Clone repository**
   ```bash
   git clone <repo-url> /opt/insurance-app
   cd /opt/insurance-app
   ```

3. **Configure production environment**
   ```bash
   cp .env.example .env
   # Edit with production values
   nano .env
   ```

4. **Deploy**
   ```bash
   docker-compose up -d
   ```

### Option 2: Kubernetes Deployment

Kubernetes manifests can be generated from docker-compose.yml using tools like Kompose.

Basic steps:
1. Convert: `kompose convert -f docker-compose.yml`
2. Apply secrets: `kubectl apply -f secrets.yml`
3. Deploy: `kubectl apply -f .`

## 🔐 Security Considerations

### Authentication
- JWT tokens with configurable expiration
- BCrypt password hashing
- Role-based access control (ADMIN/AGENT)

### Data Protection
- Admin settings encrypted at rest (Base64 encoding - use stronger encryption in production)
- Environment variables for sensitive data
- HTTPS recommended for production
- Input validation on all endpoints

### API Security
- Rate limiting (configure in API Gateway)
- CORS configured for known origins
- SQL injection prevention via JPA
- XSS protection headers

## 📈 Monitoring & Observability

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Metrics (Prometheus format)
curl http://localhost:8080/actuator/metrics
```

### Logs

```bash
# View all logs
docker-compose logs -f

# Backend logs
docker-compose logs -f backend

# Database logs
docker-compose logs -f postgres
```

### Audit Trail

All admin actions are logged in the `audit_logs` table:
- User management changes
- Product modifications
- Settings updates
- Before/after state

Access via Admin UI → Audit Logs

## 🧪 Testing

### Backend Tests

```bash
cd backend
mvn test
```

Test categories:
- Unit tests (services, utilities)
- Integration tests (repositories, controllers)
- Security tests (auth, authorization)

### Frontend Tests

```bash
cd frontend
npm run test
```

## 🔄 CI/CD Pipeline

### GitHub Actions Workflows

**1. CI (Continuous Integration)** - `.github/workflows/ci.yml`
- Triggers: Pull requests, pushes to develop
- Jobs:
  - Backend build & test
  - Frontend build & test
  - Docker image build test

**2. CD (Continuous Deployment)** - `.github/workflows/cd.yml`
- Triggers: Push to main
- Jobs:
  - Build Docker images
  - Push to GitHub Container Registry (GHCR)

**3. Deploy** - `.github/workflows/deploy.yml`
- Triggers: After successful CD, manual dispatch
- Jobs:
  - SSH to production VM
  - Pull latest images
  - Restart services
  - Health check

### Required Secrets

Configure in GitHub Settings → Secrets:

```
# Deployment
SSH_HOST=your.server.com
SSH_USER=deploy
SSH_PRIVATE_KEY=<your-ssh-private-key>
SSH_PORT=22

# Application
DB_PASSWORD=<production-db-password>
JWT_SECRET=<production-jwt-secret>
OPENAI_API_KEY=<openai-api-key>
TWILIO_ACCOUNT_SID=<twilio-sid>
TWILIO_AUTH_TOKEN=<twilio-token>
SMTP_USERNAME=<smtp-user>
SMTP_PASSWORD=<smtp-pass>
```

## 📖 API Documentation

Once running, access interactive API documentation:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

### Key Endpoints

**Authentication:**
- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Register (admin only)
- `GET /api/auth/me` - Get current user

**Leads:**
- `GET /api/leads` - List leads (paginated, filtered)
- `POST /api/leads` - Create lead
- `GET /api/leads/{id}` - Get lead details
- `PUT /api/leads/{id}` - Update lead
- `POST /api/leads/import` - CSV import

**Voice Sessions:**
- `POST /api/voice/sessions/start` - Start AI voice call
- `POST /api/voice/sessions/{id}/stop` - Stop session
- `GET /api/voice/sessions/{id}` - Get transcript & recommendations

**Products:**
- `GET /api/products` - List products
- `GET /api/products/search` - Search products
- `POST /api/products` - Create product (admin)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License.

## 🆘 Support & Troubleshooting

### Known Issues

**⚠️ Local Maven Compilation with Java 21.0.5**

If you encounter a `TypeTag :: UNKNOWN` error when running `mvn compile` locally, this is a known Lombok + Java 21.0.5 compatibility issue.

**Solution:** Use Docker build (recommended):
```bash
./run-dev.sh
# OR
docker-compose up --build
```

The Docker build uses a controlled Java environment and works correctly. See `KNOWN_ISSUES.md` for alternative solutions.

### Common Issues

**Issue: Database connection failed**
```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check logs
docker-compose logs postgres

# Restart database
docker-compose restart postgres
```

**Issue: OpenAI API errors**
- Verify API key is correct
- Check API quota/billing
- Ensure model names are correct

**Issue: Frontend can't connect to backend**
- Check VITE_API_URL in .env
- Verify backend is running: `curl http://localhost:8080/actuator/health`
- Check browser console for CORS errors

### Getting Help

- GitHub Issues: Report bugs and request features
- Documentation: Check `/docs` folder for detailed guides
- Logs: Always check application logs first

## 🎓 Learning Resources

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [OpenAI Realtime API Guide](https://platform.openai.com/docs/guides/realtime)
- [pgvector Documentation](https://github.com/pgvector/pgvector)
- [React Documentation](https://react.dev/)

---

**Built with ❤️ for Insurance Agents**

*Empowering insurance professionals with AI-driven sales tools*
