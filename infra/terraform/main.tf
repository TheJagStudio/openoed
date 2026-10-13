# Reference only. Not applied.
terraform {
  required_version = ">= 1.6.0"
}

# Sketch of the production shape. The running system is the local platform stand-in.
# Aurora PostgreSQL, SQS, EventBridge Scheduler, Lambda, SES and an append-only DynamoDB ledger.
# See docs/AWS.md.
