# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the Spring Boot jar with the Maven wrapper ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached until pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q -DskipTests package && cp target/leave-management-*.jar app.jar

# ---- Runtime stage: JRE only, non-root ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /workspace/app.jar app.jar
USER app

# Render injects PORT; application.yml binds server.port to it (8080 when unset).
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
