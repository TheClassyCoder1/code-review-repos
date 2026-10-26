# Collections outreach worker: same artifact as ops-console, started with the outreach profile.
FROM maven:3.9.6-eclipse-temurin-17 AS build
USER root
WORKDIR /src
COPY pom.xml lombok.config ./
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package \
    && mkdir -p /out \
    && cp target/ops-console-*.jar /out/app.jar

FROM eclipse-temurin:17.0.11_9-jre-jammy
RUN groupadd --system --gid 10001 outreach \
    && useradd --system --uid 10001 --gid outreach --home-dir /opt/outreach --shell /usr/sbin/nologin outreach
WORKDIR /opt/outreach
COPY --from=build --chown=outreach:outreach /out/app.jar app.jar
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=outreach \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/outreach/app.jar"]
