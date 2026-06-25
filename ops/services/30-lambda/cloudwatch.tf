# Alarms that detect when coverage counts stop flowing into the lambda.coverage_counts table
# (surfaced downstream as the ab2d_prod_coverage_counts QuickSight dataset / view).
#
# Coverage counts reach the table via this path:
#   worker -> SNS topic "${env}-coverage-count" -> coverage-counts lambda -> INSERT INTO lambda.coverage_counts
#
# If the worker stops publishing (as happened when the AB2D coverage-count call was commented out),
# the coverage-counts lambda stops being invoked and no new rows are written. Both alarms below use the
# native AWS/Lambda metrics for the coverage-counts handler so we are alerted when that happens.

data "aws_sns_topic" "cloudwatch_alarms" {
  name = "${local.service_prefix}-cloudwatch-alarms"
}

# Fires when the coverage-counts lambda has not been invoked at all over a 24 hour window. Under normal
# operation the BFD per-period path and the AB2D bulk path both publish coverage counts regularly, so a
# full day with zero invocations indicates publishing has stopped. Missing data is treated as breaching
# because "no invocations" shows up as missing data points for the Invocations metric.
resource "aws_cloudwatch_metric_alarm" "coverage_counts_not_published" {
  alarm_name          = "${local.service_prefix}-coverage-counts-not-published"
  alarm_description   = "No coverage counts have been published to the ${aws_lambda_function.coverage_count.function_name} lambda in the last 24 hours; the ab2d_prod_coverage_counts view will go stale."
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Invocations"
  namespace           = "AWS/Lambda"
  period              = "86400"
  statistic           = "Sum"
  threshold           = "1.0"
  treat_missing_data  = "breaching"
  alarm_actions       = [data.aws_sns_topic.cloudwatch_alarms.arn]
  ok_actions          = [data.aws_sns_topic.cloudwatch_alarms.arn]
  dimensions = {
    FunctionName = aws_lambda_function.coverage_count.function_name
  }
}

# Fires when the coverage-counts lambda is being invoked but failing to write rows (e.g. a DB or
# deserialization error), so the SNS messages arrive but the table is not updated.
resource "aws_cloudwatch_metric_alarm" "coverage_counts_lambda_errors" {
  alarm_name          = "${local.service_prefix}-coverage-counts-lambda-errors"
  alarm_description   = "The ${aws_lambda_function.coverage_count.function_name} lambda is erroring while writing coverage counts; rows may be missing from the ab2d_prod_coverage_counts view."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = "3600"
  statistic           = "Sum"
  threshold           = "0.0"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [data.aws_sns_topic.cloudwatch_alarms.arn]
  ok_actions          = [data.aws_sns_topic.cloudwatch_alarms.arn]
  dimensions = {
    FunctionName = aws_lambda_function.coverage_count.function_name
  }
}
