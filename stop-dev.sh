#!/bin/bash

# Insurance App - Stop Development Environment

echo "Stopping Insurance App Development Environment..."

# Stop Docker services (backend, frontend, postgres)
if docker info > /dev/null 2>&1; then
    echo "Stopping Docker containers..."
    docker-compose down 2>/dev/null || true
    echo "Docker containers stopped."
else
    echo "Docker is not running, skipping container shutdown."
fi

# Kill any local Vite dev server on port 3000/3001
for port in 3000 3001; do
    pids=$(lsof -ti:$port 2>/dev/null)
    if [ -n "$pids" ]; then
        echo "Stopping process on port $port (PID: $pids)..."
        echo "$pids" | xargs kill 2>/dev/null
    fi
done

echo "All services stopped."
