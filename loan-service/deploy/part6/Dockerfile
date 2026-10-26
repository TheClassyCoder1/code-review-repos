# loan-service image. Build from the loan-service directory:
#   docker build -f deploy/part6/Dockerfile -t registry.internal/lending/loan-service:<git-sha> .

FROM maven:3.9.6-eclipse-temurin-17 AS build
# The Maven image runs as root; the protobuf plugin writes into /root/.m2 during the build.
USER root
WORKDIR /build
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package && cp target/loan-service-*.jar /build/app.jar

FROM eclipse-temurin:17.0.10_7-jre-jammy
RUN groupadd --system --gid 10001 loan \
 && useradd --system --uid 10001 --gid loan --no-create-home --shell /usr/sbin/nologin loan
WORKDIR /app
COPY --from=build --chown=loan:loan /build/app.jar ./app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
