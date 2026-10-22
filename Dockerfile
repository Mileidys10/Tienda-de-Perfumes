FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw package -DskipTests

# Stage 2: Runtime image with non-root security hardening
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Crear usuario y grupo sin privilegios de root (Principle of Least Privilege)
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser && \
    mkdir -p /app/uploads /app/data && \
    chown -R appuser:appgroup /app

COPY --from=builder /workspace/app/target/*.jar app.jar
RUN chown appuser:appgroup /app/app.jar

# Cambiar a usuario no root antes de la ejecucion
USER appuser:appgroup

ENV PORT 8080
EXPOSE $PORT

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/api/perfumes || exit 1

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
