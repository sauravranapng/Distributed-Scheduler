# ---------- Build Stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching
COPY pom.xml .

# Download project dependencies
RUN mvn dependency:go-offline

# Copy application source
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:21-jre

#Containers provide isolation, but they share the host kernel.
#If a container is compromised, running as a non-root user reduces what an attacker can do inside that container.
#It doesn't eliminate all risk, but it limits the damage and follows the principle of least privilege—a core security practice.
RUN useradd -ms /bin/bash spring

WORKDIR /app

# Copy the generated JAR from the build stage
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]