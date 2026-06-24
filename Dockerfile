FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app/backend
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM node:22-alpine AS frontend-build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend-build /app/backend/target/api-test-designer-backend-0.1.0.jar app.jar
COPY --from=frontend-build /app/dist ./static
ENV SPRING_WEB_RESOURCES_STATIC_LOCATIONS=classpath:/static/,file:/app/static/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
