# loan-service image. Build from the loan-service directory:
#   docker build -f deploy/part6/Dockerfile -t registry.internal/lending/loan-service:1.6.0 .

# ---- build stage -------------------------------------------------------------
# Runs as root: Maven needs to write to /root/.m2 and the image's default workdir.
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package \
    && cp target/loan-service-*.jar /workspace/app.jar

# ---- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17.0.11_9-jre-jammy

RUN groupadd --system --gid 10001 lending \
    && useradd --system --uid 10001 --gid lending --home-dir /app --shell /usr/sbin/nologin lending

WORKDIR /app
COPY --from=build --chown=lending:lending /workspace/app.jar /app/app.jar

USER 10001:10001

EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
