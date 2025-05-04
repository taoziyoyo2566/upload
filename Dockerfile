FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first for better caching
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create directories for file uploads
RUN mkdir -p /app/uploads /app/filedb

# Set volume mount points
VOLUME ["/app/uploads", "/app/filedb"]

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]