#!/bin/bash

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
until echo exit | nc localhost 9092; do
    echo "Waiting for Kafka to be ready..."
    sleep 2
done

# Read topics configuration
TOPICS_CONFIG=$(cat /kafka/topics.json)

# Function to create a topic
create_topic() {
    local topic_name=$1
    local partitions=$2
    local replication_factor=$3
    local configs=$4

    echo "Creating topic: $topic_name"
    kafka-topics \
        --create \
        --if-not-exists \
        --bootstrap-server localhost:9092 \
        --topic "$topic_name" \
        --partitions "$partitions" \
        --replication-factor "$replication_factor" \
        --config cleanup.policy="${configs[cleanup.policy]}" \
        --config retention.ms="${configs[retention.ms]}" \
        --config segment.bytes="${configs[segment.bytes]}" \
        --config min.insync.replicas="${configs[min.insync.replicas]}"
}

# Create topics from configuration
echo "$TOPICS_CONFIG" | jq -c '.topics[]' | while read -r topic; do
    name=$(echo "$topic" | jq -r '.name')
    partitions=$(echo "$topic" | jq -r '.partitions')
    replication_factor=$(echo "$topic" | jq -r '.replicationFactor')
    configs=$(echo "$topic" | jq -r '.configs')
    
    create_topic "$name" "$partitions" "$replication_factor" "$configs"
done

echo "Topics created successfully!"
