#!/bin/bash

set -e

CONFIG_FILE=/opt/kafka/config/kraft/server.properties
DATA_DIR=/var/lib/kafka/data

echo "Checking Kafka storage..."

if [ ! -f "$DATA_DIR/meta.properties" ]; then
    echo "Formatting Kafka storage..."

    /opt/kafka/bin/kafka-storage.sh format \
        --ignore-formatted \
        --cluster-id "$KAFKA_CLUSTER_ID" \
        --config "$CONFIG_FILE"

    echo "Kafka storage formatted."
else
    echo "Kafka storage already formatted."
fi

echo "Starting Kafka..."

exec /opt/kafka/bin/kafka-server-start.sh "$CONFIG_FILE"