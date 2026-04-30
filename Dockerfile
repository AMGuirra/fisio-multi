FROM maven:3.9.14-eclipse-temurin-25 AS build

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests \
    && JAR_FILE=$(find target -maxdepth 1 -type f -name "*.jar" ! -name "original-*.jar" | head -n 1) \
    && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:25

WORKDIR /app

EXPOSE 8083

COPY --from=build /app/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
