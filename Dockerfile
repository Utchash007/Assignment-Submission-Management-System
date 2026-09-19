# ============================================================
# Stage 1: Build Spring Boot Backend (JDK 25 LTS)
# ============================================================
FROM eclipse-temurin:25-jdk AS backend-builder
WORKDIR /build/backend

# Copy wrapper + pom first for readability (single build step keeps
# cloud rebuilds robust; tests run locally, so the image skips them).
COPY Java-Backend/spring-asms/mvnw Java-Backend/spring-asms/mvnw.cmd Java-Backend/spring-asms/pom.xml ./
COPY Java-Backend/spring-asms/.mvn ./.mvn
COPY Java-Backend/spring-asms/src ./src
RUN mkdir -p /app && chmod +x mvnw && ./mvnw -q package -DskipTests \
 && cp target/spring-asms-*.jar /app/backend.jar

# ============================================================
# Stage 2: Build Next.js 15 Frontend
# ============================================================
FROM node:20-alpine AS frontend-builder
WORKDIR /app

# Copy package dependencies and install
COPY Frontend/package.json Frontend/package-lock.json* ./
RUN npm install

# Copy frontend source and build standalone bundle
COPY Frontend/ .
RUN mkdir -p public
ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_ENV=production
RUN npm run build

# ============================================================
# Stage 3: Unified Production Runtime (Backend + Frontend)
# ============================================================
FROM eclipse-temurin:25-jre AS runner
WORKDIR /app

# Install Node.js runtime for Next.js
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y --no-install-recommends nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy backend jar
COPY --from=backend-builder /app/backend.jar /app/backend/app.jar

# Copy frontend standalone output & assets
COPY --from=frontend-builder /app/public /app/frontend/public
COPY --from=frontend-builder /app/.next/standalone /app/frontend/
COPY --from=frontend-builder /app/.next/static /app/frontend/.next/static

# Environment settings (Render injects PORT; DB/JWT secrets come from
# the Render dashboard — see Java-Backend/08-migration-checklist.md Phase 8)
ENV SPRING_PROFILES_ACTIVE=prod
# Render free tier has 512MB RAM shared by both processes: keep the JVM small
# (heap 256m + metaspace cap) and cap Node too, or the backend gets OOM-killed
# while the frontend keeps serving (dead /api/* with a live site).
ENV JAVA_OPTS=-Xmx256m -XX:MaxMetaspaceSize=128m
ENV NODE_OPTIONS=--max-old-space-size=256
ENV INTERNAL_BACKEND_URL=http://127.0.0.1:5000
ENV NEXT_PUBLIC_API_URL=http://127.0.0.1:5000
ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=10000

EXPOSE 10000 3000 5000

# Direct inline command: binds Next.js to 0.0.0.0:$PORT so Render can route traffic
CMD ["/bin/bash", "-c", "cd /app/backend && java $JAVA_OPTS -jar app.jar & cd /app/frontend && HOSTNAME=0.0.0.0 PORT=${PORT:-10000} node server.js"]
