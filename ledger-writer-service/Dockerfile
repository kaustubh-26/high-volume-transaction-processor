FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

COPY . .
RUN chmod +x mvnw && ./mvnw -pl ledger-writer-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /build/ledger-writer-service/target/ledger-writer-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
