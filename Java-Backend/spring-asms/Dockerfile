# ============================================================
# Stage 1: Build Spring Boot backend (JDK 25 LTS)
# ============================================================
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn ./.mvn
COPY src ./src
RUN mkdir -p /app && chmod +x mvnw && ./mvnw -q package -DskipTests \
 && cp target/spring-asms-*.jar /app/app.jar

# ============================================================
# Stage 2: Production runtime (JRE 25)
# ============================================================
FROM eclipse-temurin:25-jre AS runner
WORKDIR /app/backend

COPY --from=builder /app/app.jar ./app.jar

EXPOSE 5000
ENV PORT=5000
ENV JAVA_OPTS=-Xmx384m
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
