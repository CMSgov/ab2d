locals {
  aws_monitor_scope = "application:${local.app},environment:${local.env}"

  # Replaces the CDAP module's Lambda monitors, which force require_full_window = true. AB2D's
  # functions run a few times a day, so their windows are never full and those monitors never left
  # No Data.
  lambda_monitors = [
    {
      name    = "[${upper(local.env)}] [${local.app}] Lambda — Errors"
      type    = "query alert"
      message = "Lambda function {{functionname.name}} reported {{value}} errors in the last hour. Check the function's CloudWatch log group for the failing invocation."
      query   = "sum(last_1h):sum:aws.lambda.errors{${local.aws_monitor_scope}} by {functionname}.as_count() > 0"
      thresholds = {
        critical = 0
      }
      tags = ["service:lambda"]
    },
  ]

  # Replaces the CDAP module's SQS monitors. Its DLQ monitor filters on queuename:*dlq*, but AB2D's
  # dead-letter queues are named *-dead-letter, so it never matched a queue.
  sqs_monitors = [
    {
      name    = "[${upper(local.env)}] [${local.app}] SQS — DLQ Messages Visible"
      type    = "query alert"
      message = "Dead-letter queue {{queuename.name}} has {{value}} messages. Messages that failed processing were moved there and will not be retried on their own."
      query   = "max(last_15m):max:aws.sqs.approximate_number_of_messages_visible{${local.aws_monitor_scope},queuename:*-dead-letter} by {queuename} > 0"
      thresholds = {
        critical = 0
      }
      tags = ["service:sqs"]
    },
    {
      name    = "[${upper(local.env)}] [${local.app}] SQS — Message Age Too High"
      type    = "query alert"
      message = "The oldest message in queue {{queuename.name}} is {{value}} seconds old. The consumer is down or falling behind."
      query   = "avg(last_5m):avg:aws.sqs.approximate_age_of_oldest_message{${local.aws_monitor_scope},!queuename:*-dead-letter} by {queuename} > 300"
      thresholds = {
        critical = 300
        warning  = 225
      }
      tags = ["service:sqs"]
    },
  ]
}
