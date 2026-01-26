# Build stage (independent of mvnw location)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -f server/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/server/target/elektropregled-server-1.0.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
CMD ["java", "-jar", "app.jar"]