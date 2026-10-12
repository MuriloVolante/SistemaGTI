FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src/ ./src/
RUN rm -rf src/main/resources/static
COPY --from=frontend /app/src/main/resources/static/ ./src/main/resources/static/
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S gti && adduser -S gti -G gti
WORKDIR /app
COPY --from=backend /build/target/gti-1.0.0.jar app.jar
RUN chown -R gti:gti /app
USER gti
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]