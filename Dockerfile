# Etapa 1: Compilar con Maven
FROM maven:3.9.4-amazoncorretto-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Crear la imagen final ligera
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY --from=build /app/target/user-service-0.0.1-SNAPSHOT.jar app.jar

# Render expone automáticamente el puerto 8090, asegúrate de usarlo
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]