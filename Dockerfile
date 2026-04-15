# syntax=docker/dockerfile:1.7
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .

# Cache dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# --- FIXED THIS LINE ---
COPY src ./src

# Build the package
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# Stage 2: Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

# Copy the JAR
COPY --from=build /app/target/*.jar app.jar

RUN chown spring:spring app.jar
USER spring

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]