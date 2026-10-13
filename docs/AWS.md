# AWS mapping (reference only — nothing is deployed)

| Local | AWS |
|---|---|
| oed-api | ECS Fargate behind an ALB. Task role has `sqs:SendMessage`, `scheduler:CreateSchedule`, `scheduler:DeleteSchedule`, `dynamodb:PutItem` on the ledger. |
| PostgreSQL 18 schema `oed` | Aurora PostgreSQL. Credentials from Secrets Manager. |
| platform SQS | Amazon SQS queue `office-actions` |
| platform schedules | EventBridge Scheduler one-shot, six months out, target the abandonment Lambda |
| deadline-lambda | Lambda Java 21, SnapStart. Calls the internal API and SES. |
| platform SES outbox | Amazon SES |
| `data/platform/audit-ledger.jsonl` | DynamoDB table `audit-ledger`, partition key `pk`, sort key `sk`. IAM denies `UpdateItem` and `DeleteItem`. |
| web `dist/` | S3 + CloudFront |

Terraform sketches live in `infra/terraform` and are not applied.
