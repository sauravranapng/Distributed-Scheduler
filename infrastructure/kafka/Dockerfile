FROM apache/kafka:3.9.1

USER root

COPY server.properties /opt/kafka/config/kraft/server.properties
COPY start-kafka.sh /usr/local/bin/start-kafka.sh

RUN chmod +x /usr/local/bin/start-kafka.sh

USER appuser

ENTRYPOINT ["/usr/local/bin/start-kafka.sh"]