FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/nytimes-crawler-1.0-SNAPSHOT-shaded.jar ./crawler.jar
CMD ["java", "-jar", "crawler.jar"]