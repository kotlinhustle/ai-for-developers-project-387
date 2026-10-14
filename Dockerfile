# syntax=docker/dockerfile:1

# --- 1. Generate API artifacts: TypeSpec -> OpenAPI -> TS client + Kotlin server ---
FROM node:24-slim AS spec
WORKDIR /src
RUN apt-get update \
    && apt-get install -y --no-install-recommends default-jre-headless \
    && rm -rf /var/lib/apt/lists/*
COPY spec/package.json spec/package-lock.json ./spec/
RUN npm --prefix spec ci
COPY spec/ ./spec/
RUN npm --prefix spec run generate

# --- 2. Build the frontend (uses the generated TS client) ---
FROM node:24-slim AS frontend
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./frontend/
RUN npm --prefix frontend ci
COPY frontend/ ./frontend/
COPY --from=spec /src/frontend/src/generated ./frontend/src/generated
# Same-origin API: the backend serves the frontend, so no /api prefix (no dev proxy).
RUN VITE_API_BASE_PATH= npm --prefix frontend run build

# --- 3. Build the backend (generated Kotlin + frontend dist as static resources) ---
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /src
COPY backend/ ./backend/
COPY --from=spec /src/backend/build/generated ./backend/build/generated
COPY --from=frontend /src/frontend/dist ./backend/src/main/resources/static
RUN cd backend && chmod +x gradlew && ./gradlew bootJar --no-daemon && rm -f build/libs/*-plain.jar

# --- 4. Runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /src/backend/build/libs/backend-*.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
