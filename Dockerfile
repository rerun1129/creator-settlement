# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY src src
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home --home /app app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
