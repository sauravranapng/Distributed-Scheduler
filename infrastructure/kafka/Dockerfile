# Start from the official Apache Kafka image.
# This image already contains:
# - Linux file system
# - Java Runtime
# - Apache Kafka binaries
# - Kafka startup and administration scripts
FROM apache/kafka:3.9.1

# Temporarily switch to the root user.
# Root privileges are required to copy files into protected system directories
# and modify file permissions.
USER root

# Replacing Kafka's default KRaft configuration with our own configuration.
# This allows us to fully control the broker settings instead of relying on
# the defaults provided by the base image.
COPY server.properties /opt/kafka/config/kraft/server.properties

# Copy our custom startup script into the container.
# This script formats Kafka storage (only on first startup) and then starts
# the Kafka broker.
COPY start-kafka.sh /usr/local/bin/start-kafka.sh

# Make the startup script executable.
# Without this permission, Linux will not allow Docker to execute the script.
RUN chmod +x /usr/local/bin/start-kafka.sh

# Switch back to the non-root user provided by the official Kafka image.
# Running containers as a non-root user is a security best practice because
# it limits the privileges available to the Kafka process.
USER appuser

# Override the default Kafka entrypoint.
# Instead of directly starting Kafka, Docker executes our startup script,
# which performs one-time KRaft storage initialization before launching
# the Kafka broker.
ENTRYPOINT ["/usr/local/bin/start-kafka.sh"]