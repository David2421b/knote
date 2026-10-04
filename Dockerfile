FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV PORT=3000
EXPOSE 3000
COPY --from=build /workspace/target/knote-1.0.0.jar /app/knote.jar
ENTRYPOINT ["java", "-jar", "/app/knote.jar"]
