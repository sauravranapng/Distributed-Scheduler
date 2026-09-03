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

/opt/kafka/bin/kafka-server-start.sh "$CONFIG_FILE" &

KAFKA_PID=$!

echo "Waiting for Kafka..."

until /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 \
    --list >/dev/null 2>&1
do
    sleep 2
done

echo "Kafka is ready."

create_or_update_topic() {
    local topic=$1
    local desired_partitions=$2

    echo "Checking topic: $topic"

    if /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --list | grep -Fxq "$topic"; then

        current_partitions=$(
            /opt/kafka/bin/kafka-topics.sh \
                --bootstrap-server localhost:9092 \
                --describe \
                --topic "$topic" |
            grep "PartitionCount:" |
            sed -n 's/.*PartitionCount: \([0-9]*\).*/\1/p'
        )

        echo "Topic $topic exists with $current_partitions partitions."

        if [ "$current_partitions" -lt "$desired_partitions" ]; then

            echo "Increasing $topic partitions from $current_partitions to $desired_partitions..."

            /opt/kafka/bin/kafka-topics.sh \
                --bootstrap-server localhost:9092 \
                --alter \
                --topic "$topic" \
                --partitions "$desired_partitions"

            echo "Topic $topic updated."

        elif [ "$current_partitions" -eq "$desired_partitions" ]; then

            echo "Topic $topic already has $desired_partitions partitions."

        else

            echo "Topic $topic has $current_partitions partitions, which is greater than the desired $desired_partitions. No change."

        fi

    else

        echo "Topic $topic does not exist. Creating..."

        /opt/kafka/bin/kafka-topics.sh \
            --bootstrap-server localhost:9092 \
            --create \
            --topic "$topic" \
            --partitions "$desired_partitions" \
            --replication-factor 1

        echo "Topic $topic created."

    fi
}

echo "Configuring Kafka topics..."

create_or_update_topic "scheduling-topic" 4
create_or_update_topic "scheduling-topic-retry-60000" 4
create_or_update_topic "scheduling-topic-retry-300000" 4
create_or_update_topic "scheduling-topic-retry-1500000" 4
create_or_update_topic "scheduling-topic-dlt" 4

echo "Kafka topics configured successfully."

wait "$KAFKA_PID"