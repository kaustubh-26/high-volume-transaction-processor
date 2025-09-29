#!/bin/bash
set -e

echo "Waiting for Kafka..."

until /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list >/dev/null 2>&1; do
  echo "Kafka not ready, retrying..."
  sleep 2
done

echo "Creating topics..."

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:29092 \
  --create --if-not-exists \
  --topic transaction_requests \
  --replication-factor 1 \
  --partitions 6

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:29092 \
  --create --if-not-exists \
  --topic transaction_log \
  --replication-factor 1 \
  --partitions 6

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create --if-not-exists \
  --topic transaction_requests_dlt \
  --replication-factor 1 \
  --partitions 6

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create --if-not-exists \
  --topic transaction_log_dlt \
  --replication-factor 1 \
  --partitions 6


echo "Available topics:"
/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:29092 \
  --list
