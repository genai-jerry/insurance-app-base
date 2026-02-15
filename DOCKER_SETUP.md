# Docker Setup (Colima)

This project uses **Colima** as the Docker runtime on macOS (no Docker Desktop).

## Start Docker

```bash
colima start
```

## Stop Docker

```bash
colima stop
```

## Restart Docker (if Docker commands fail)

```bash
colima stop && colima start
```

## Check Status

```bash
# Check if Colima VM is running
colima status

# Check if Docker daemon is accessible
docker info > /dev/null 2>&1 && echo "Running" || echo "Not running"
```

## Troubleshooting

**`docker: command not found`** — Install the Docker CLI:
```bash
brew install docker docker-compose
```

**`colima: command not found`** — Install Colima:
```bash
brew install colima
```

**Docker commands hang or fail after Colima says "already running"** — Force restart:
```bash
colima stop && colima start
```
