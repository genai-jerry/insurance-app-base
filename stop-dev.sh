#!/bin/bash

# Insurance App - Stop Development Environment
# Stops all app containers (backend, frontend, postgres) but leaves Docker daemon running

echo "Stopping Insurance App Development Environment..."

if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running, skipping container shutdown."
    exit 0
fi

# Stop and remove only the insurance app containers
echo "Stopping insurance app containers..."
docker-compose stop 2>/dev/null || true
docker-compose rm -f 2>/dev/null || true

# Also remove any orphaned insurance containers not managed by compose
for container in insurance-backend insurance-frontend insurance-postgres; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "Removing orphaned container: ${container}..."
        docker stop "$container" 2>/dev/null || true
        docker rm "$container" 2>/dev/null || true
    fi
done

# Kill any local Vite dev server on port 3000/3001
for port in 3000 3001; do
    pids=$(lsof -ti:$port 2>/dev/null)
    if [ -n "$pids" ]; then
        echo "Stopping process on port $port (PID: $pids)..."
        echo "$pids" | xargs kill 2>/dev/null
    fi
done

echo "All services stopped. Docker daemon is still running."
