#!/bin/bash

# Insurance App Local Development Script

set -e

echo "🚀 Starting Insurance App Development Environment..."

# Check if .env exists, if not copy from .env.example
if [ ! -f .env ]; then
    echo "📝 Creating .env file from .env.example..."
    cp .env.example .env
    echo "⚠️  Please edit .env file with your actual credentials before proceeding!"
    echo "Press Enter to continue after editing .env, or Ctrl+C to exit..."
    read
fi

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

echo "🧹 Cleaning up previous containers..."
docker-compose down -v 2>/dev/null || true

echo "🏗️  Building and starting services..."
docker-compose up --build -d postgres

echo "⏳ Waiting for PostgreSQL to be ready..."
until docker-compose exec -T postgres pg_isready -U ${DB_USER} > /dev/null 2>&1; do
    sleep 2
done

echo "✅ PostgreSQL is ready!"

echo "🏗️  Building backend..."
docker-compose up --build -d backend

echo "⏳ Waiting for backend to be ready..."
until curl -f http://localhost:${BACKEND_PORT:-8080}/actuator/health > /dev/null 2>&1; do
    sleep 3
done

echo "✅ Backend is ready!"

echo "🏗️  Building frontend..."
docker-compose up --build -d frontend

echo "
✅ All services are up and running!

📊 Services:
  - Frontend:  http://localhost:${FRONTEND_PORT:-3000}
  - Backend:   http://localhost:${BACKEND_PORT:-8080}
  - API Docs:  http://localhost:${BACKEND_PORT:-8080}/swagger-ui.html
  - Database:  localhost:5432

🔐 Default credentials:
  - Admin:     admin@insurance.com / Admin@123
  - Agent:     agent@insurance.com / Agent@123

📝 Logs:
  - All:       docker-compose logs -f
  - Backend:   docker-compose logs -f backend
  - Frontend:  docker-compose logs -f frontend

🛑 Stop services:
  docker-compose down
"
