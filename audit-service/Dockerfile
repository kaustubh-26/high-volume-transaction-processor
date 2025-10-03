FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

COPY . .
RUN chmod +x mvnw && ./mvnw -pl audit-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /build/audit-service/target/audit-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
