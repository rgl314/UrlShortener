FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /url-shortener

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /url-shortener

COPY --from=builder /url-shortener/target/*.jar url-shortener.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","url-shortener.jar"]