# ops-console image: build with Maven, run on a slim JRE.
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml lombok.config ./
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17.0.11_9-jre-jammy
WORKDIR /opt/ops-console
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /src/target/ops-console-*.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/ops-console/app.jar"]
