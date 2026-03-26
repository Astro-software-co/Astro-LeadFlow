#!/bin/bash
echo "=== Inicializando recursos AWS locais ==="

awslocal sqs create-queue \
  --queue-name leadflow-new-leads.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false

awslocal sqs create-queue \
  --queue-name marketplace-leads.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false \
  --region us-east-1

awslocal s3 mb s3://leadflow-vehicles

echo "=== Recursos criados ==="
awslocal sqs list-queues
awslocal s3 ls