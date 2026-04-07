# Etapa 1: Build del frontend (Vue + Vite)
FROM node:22-alpine AS frontend-builder

WORKDIR /frontend

# Cache de dependencias
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# Código del frontend y build de producción
COPY frontend/ ./
RUN npm run build


# Etapa 2: Build del backend (Spring Boot + Maven)
FROM maven:3.9.9-eclipse-temurin-21-jammy AS backend-builder

WORKDIR /project

# Cache de dependencias Maven
COPY pom.xml ./
RUN mvn -B dependency:go-offline

# Código del backend
COPY src ./src

# Inyecta el frontend compilado en recursos estáticos de Spring
COPY --from=frontend-builder /frontend/dist ./src/main/resources/static

# Compila y reempaqueta como JAR ejecutable de Spring Boot
RUN mvn -B clean package spring-boot:repackage -DskipTests \
	-Dskip.frontend.node.install=true \
	-Dskip.frontend.npm.install=true \
	-Dskip.frontend.npm.build=true \
	-Dskip.frontend.copy.resources=true


# Etapa 3: Imagen final runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /usr/src/app

# Copia el JAR generado
COPY --from=backend-builder /project/target/*.jar /usr/src/app/app.jar

# Puerto del backend Spring Boot
EXPOSE 8090

# Arranque de la aplicación
ENTRYPOINT ["java", "-jar", "/usr/src/app/app.jar"]
