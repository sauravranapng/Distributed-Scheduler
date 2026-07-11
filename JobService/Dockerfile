# ---------- Build Stage ----------

# Start from an image that already contains:
# - Linux fileSystem
# - Eclipse Temurin JDK 21
# - Maven 3.9.9
#
# Maven is required because it compiles the source code, resolves dependencies,
# runs the build lifecycle, and packages the application into an executable JAR.
FROM maven:3.9.9-eclipse-temurin-21 AS build

# Creates (if it doesn't already exist) and switches to the /app directory
# inside the container. All subsequent commands execute relative to this path.
WORKDIR /app

# Copy pom.xml first instead of the entire project.
#
# Docker caches every build layer. Since project dependencies are defined in
# pom.xml, Docker can reuse the cached dependency layer as long as pom.xml
# remains unchanged. If only the source code changes, Maven dependencies are
# not downloaded again, significantly reducing build time.
COPY pom.xml .

# Downloads all project dependencies and stores them in Maven's local cache
# inside the build image.
RUN mvn dependency:go-offline

# Copy the application source code.
COPY src ./src

# Compile and package the application into an executable JAR.
# Tests are skipped to reduce build time.
RUN mvn clean package -DskipTests


# ---------- Runtime Stage ----------

# Create a fresh runtime image containing only:
# - Linux operating system
# - Eclipse Temurin Java 21 Runtime (JRE)
#
# Maven is intentionally omitted because it is only needed during the build.
FROM eclipse-temurin:21-jre

#Containers provide isolation, but they share the host kernel.
#If a container is compromised, running as a non-root user reduces what an attacker can do inside that container.
#It doesn't eliminate all risk, but it limits the damage and follows the principle of least privilege—a core security practice.
RUN useradd -ms /bin/bash spring

# Set the working directory.
WORKDIR /app

# Copy the generated JAR from the build stage.
# The --chown option changes ownership of the JAR to the 'spring' user so the
# application files belong to the user that executes them.
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Switch from the root user to the non-root 'spring' user.
# Any process started after this instruction will execute with the permissions
# of the spring user instead of root.
USER spring

# The Spring Boot application listens on the port configured by server.port.
# EXPOSE only documents the intended container port. The actual host-to-container
# port mapping is configured when running the container using:
# docker run -p <host-port>:<container-port>
EXPOSE 8084

# Command executed when the container starts.
# JAVA_OPTS allows JVM options (e.g., heap size) to be configured without
# rebuilding the image.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]