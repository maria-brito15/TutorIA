FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/tutoria-1.0.0.jar.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
