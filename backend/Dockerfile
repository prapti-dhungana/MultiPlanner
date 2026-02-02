# Use Maven with Java 21 to build the Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching
COPY pom.xml .

# Download dependencies 
RUN mvn -q -DskipTests dependency:go-offline

# Copy application source
COPY src ./src

# Build the application jar
RUN mvn -q -DskipTests package

# Java 21 runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose backend port 
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
