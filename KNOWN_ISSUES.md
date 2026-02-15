# Known Issues

## Lombok Compilation Issue with Java 21.0.5

**Problem:** When compiling locally with `mvn compile`, you may encounter:
```
Fatal error compiling: java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

**Cause:** This is a known compatibility issue between Lombok and certain Java 21 builds (specifically OpenJDK 21.0.5 Temurin).

**Workarounds:**

### Option 1: Use Docker Build (Recommended)
The Docker build uses a different Java environment and compiles successfully:

```bash
# Build using Docker
docker-compose build backend

# Or build and run everything
./run-dev.sh
```

### Option 2: Use Java 17
If you need to compile locally:

```bash
# Install Java 17
sdk install java 17.0.9-tem

# Use Java 17
sdk use java 17.0.9-tem

# Update pom.xml
# Change <java.version>21</java.version> to <java.version>17</java.version>

# Compile
mvn clean compile
```

###  Option 3: Use a Different Java 21 Distribution
Some Java 21 distributions work better with Lombok:

```bash
# Try Amazon Corretto 21 or Oracle JDK 21
sdk install java 21-amzn
sdk use java 21-amzn
mvn clean compile
```

### Option 4: Skip Local Compilation
You can develop and test using Docker exclusively:

```bash
# Run with Docker Compose
docker-compose up --build

# Backend will be available at http://localhost:8080
# Frontend will be available at http://localhost:3000
```

## Status

- ✅ Docker build: **WORKS**
- ✅ Application runtime: **WORKS**
- ⚠️ Local Maven build with Java 21.0.5: **Known issue**

The application is fully functional when run via Docker, which is the recommended deployment method.
