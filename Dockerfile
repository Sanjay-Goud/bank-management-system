FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src src

# Build the application using Maven directly
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]