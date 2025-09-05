#!/bin/bash

# Improved topic creation script with better error handling and retry logic

set -e

KAFKA_BROKER="localhost:9092"
MAX_RETRIES=10
RETRY_INTERVAL=5

echo "Starting topic creation process..."

# Function to wait for Kafka to be ready
wait_for_kafka() {
    echo "Waiting for Kafka to be ready..."
    local retries=0
    
    while [ $retries -lt $MAX_RETRIES ]; do
        if kafka-topics --bootstrap-server $KAFKA_BROKER --list >/dev/null 2>&1; then
            echo "Kafka is ready!"
            return 0
        fi
        
        echo "Kafka not ready yet, waiting $RETRY_INTERVAL seconds... (attempt $((retries + 1))/$MAX_RETRIES)"
        sleep $RETRY_INTERVAL
        retries=$((retries + 1))
    done
    
    echo "ERROR: Kafka failed to become ready after $MAX_RETRIES attempts"
    return 1
}

# Function to create a topic with error handling
create_topic_safe() {
    local topic_name=$1
    local partitions=$2
    local replication_factor=$3
    local cleanup_policy=$4
    local retention_ms=$5
    local segment_bytes=$6
    local min_insync_replicas=$7

    echo "Creating topic: $topic_name"
    
    # Check if topic already exists
    if kafka-topics --bootstrap-server $KAFKA_BROKER --list | grep -q "^${topic_name}$"; then
        echo "Topic $topic_name already exists, skipping creation"
        return 0
    fi
    
    # Create the topic
    if kafka-topics \
        --create \
        --bootstrap-server $KAFKA_BROKER \
        --topic "$topic_name" \
        --partitions "$partitions" \
        --replication-factor "$replication_factor" \
        --config cleanup.policy="$cleanup_policy" \
        --config retention.ms="$retention_ms" \
        --config segment.bytes="$segment_bytes" \
        --config min.insync.replicas="$min_insync_replicas"; then
        
        echo "✅ Successfully created topic: $topic_name"
        return 0
    else
        echo "❌ Failed to create topic: $topic_name"
        return 1
    fi
}

# Wait for Kafka to be ready
if ! wait_for_kafka; then
    exit 1
fi

# Read topics configuration and create topics
if [ ! -f "/kafka/topics.json" ]; then
    echo "ERROR: topics.json file not found"
    exit 1
fi

echo "Reading topics configuration from /kafka/topics.json"

# Parse JSON and create topics
jq -c '.topics[]' /kafka/topics.json | while read -r topic; do
    name=$(echo "$topic" | jq -r '.name')
    partitions=$(echo "$topic" | jq -r '.partitions')
    replication_factor=$(echo "$topic" | jq -r '.replicationFactor')
    cleanup_policy=$(echo "$topic" | jq -r '.configs["cleanup.policy"]')
    retention_ms=$(echo "$topic" | jq -r '.configs["retention.ms"]')
    segment_bytes=$(echo "$topic" | jq -r '.configs["segment.bytes"]')
    min_insync_replicas=$(echo "$topic" | jq -r '.configs["min.insync.replicas"]')
    
    if ! create_topic_safe "$name" "$partitions" "$replication_factor" "$cleanup_policy" "$retention_ms" "$segment_bytes" "$min_insync_replicas"; then
        echo "ERROR: Failed to create topic $name"
        exit 1
    fi
done

echo "🎉 All topics created successfully!"

# List all topics to verify
echo "Current topics:"
kafka-topics --bootstrap-server $KAFKA_BROKER --list

echo "Topic creation process completed!"
