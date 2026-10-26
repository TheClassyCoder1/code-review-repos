# risk-service image. Build from the risk-service directory:
#   docker build -f deploy/part6/Dockerfile -t registry.internal/lending/risk-service:1.6.0 .

FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package \
    && cp target/risk-service-*.jar /workspace/app.jar

FROM eclipse-temurin:17.0.11_9-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/risk-service
COPY --from=build /workspace/app.jar /opt/risk-service/app.jar

# HTTP API and gRPC
EXPOSE 8082 9090

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD curl -fsS http://localhost:8082/api/v1/health || exit 1

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/risk-service/app.jar"]
