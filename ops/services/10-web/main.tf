locals {
  env  = terraform.workspace
  public_subnet_ids  = [for _, v in data.aws_subnet.this : v.id]
}

module "platform" {
  source    = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform?ref=267771f3414c92e2f3090616587550e26bc41a47"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app         = "ab2d"
  env         = local.env
  root_module = "https://github.com/CMSgov/ab2d/tree/main/ops/services/10-web"
}

data "aws_vpc" "target_vpc" {
  filter {
    name   = "tag:Name"
    values = [var.legacy ? local.env : "ab2d-east-${local.env}"]
  }
}


data "aws_subnet" "this" {
  for_each = toset(data.aws_subnets.this.ids)
  id       = each.value
}

data "aws_lb_target_group" "ab2d_api" {
  name =  "api-${local.env}" 
}


resource "aws_cloudwatch_metric_alarm" "app_cpu_alarm" {
  alarm_name          = "${local.env}-app-cpu-alarm"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "300"
  statistic           = "Average"
  threshold           = "90.0"
  # alarm_actions       = [var.autoscaling_arn, var.sns_arn]
  alarm_actions       = [var.sns_arn]


  dimensions = {
    AutoScalingGroupName = var.autoscaling_name
  }
}

resource "aws_cloudwatch_metric_alarm" "target_response_time" {
  alarm_name          = "${local.env}-target-response-time"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "TargetResponseTime"
  namespace           = "AWS/ApplicationELB"
  period              = "900"
  statistic           = "Average"
  threshold           = "1.4"
  # alarm_actions       = [var.autoscaling_arn, var.sns_arn]
  alarm_actions       = [var.sns_arn]
  dimensions = {
    TargetGroup  = data.aws_lb_target_group.ab2d_api.arn_suffix
    LoadBalancer = data.aws_lb.ab2d_api.arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "healthy_host_count" {
  alarm_name          = "${local.env}-healthy-host-count"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "HealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = "300"
  statistic           = "Minimum"
  threshold           = "1.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    TargetGroup  = data.aws_lb_target_group.ab2d_api.arn_suffix
    LoadBalancer = data.aws_lb.ab2d_api.arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "app_status_check_alarm" {
  alarm_name          = "${local.env}-app-status-check-alarm"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "StatusCheckFailed"
  namespace           = "AWS/EC2"
  period              = "300"
  statistic           = "Maximum"
  threshold           = "1.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    AutoScalingGroupName = var.autoscaling_name
  }
}

resource "aws_cloudwatch_metric_alarm" "high_db_connections" {
  alarm_name          = "awsrds-${local.env}-high-db-connections"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "DatabaseConnections"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "10000.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_queue_depth" {
  alarm_name          = "awsrds-${local.env}-db-high-queue-depth"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "DiskQueueDepth"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "4.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_read_iops" {
  alarm_name          = "awsrds-${local.env}-db-high-read-iops"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "ReadIOPS"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "1700.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_read_latency" {
  alarm_name          = "awsrds-${local.env}-db-high-read-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "3"
  metric_name         = "ReadLatency"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "0.006"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_read_throughput" {
  alarm_name          = "awsrds-${local.env}-db-high-read-throughput"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "ReadThroughput"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "40000000.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_write_iops" {
  alarm_name          = "awsrds-${local.env}-db-high-write-iops"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "5"
  metric_name         = "WriteIOPS"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "1700.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_very_high_write_iops" {
  alarm_name          = "awsrds-${local.env}-db-very-high-write-iops"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "WriteIOPS"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "4500.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_write_latency" {
  alarm_name          = "awsrds-${local.env}-db-high-write-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "3"
  metric_name         = "WriteLatency"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "0.04"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_high_write_throughput" {
  alarm_name          = "awsrds-${local.env}-db-high-write-throughput"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "WriteThroughput"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "41943040.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "app_elb_http_code_elb_4xx_count" {
  alarm_name          = "${local.env}-app-elb-http-code-elb_4xx_count"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_ELB_4XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Average"
  threshold           = "10.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    LoadBalancer = var.loadbalancer_arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "http_code_elb_5xx_count" {
  alarm_name          = "${local.env}-app-elb-http-code_elb-5xx_count"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "2"
  metric_name         = "HTTPCode_ELB_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "120"
  statistic           = "Average"
  threshold           = "10.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    LoadBalancer = var.loadbalancer_arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "postgres_transaction_logs_disk_usage" {
  alarm_name          = "${local.env}-postgres-transaction-logs-disk-usage"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "15"
  metric_name         = "TransactionLogsDiskUsage"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "4000000000.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_cpu_utilization" {
  alarm_name          = "${local.env}-db-cpu-utilization"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "10"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "90.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_free_storage_space" {
  alarm_name          = "${local.env}-db-free-storage-space"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = "3600"
  statistic           = "Average"
  threshold           = "100000000000.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudwatch_metric_alarm" "db_swap_usage" {
  alarm_name          = "${local.env}-db-swap-usage"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "SwapUsage"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "1.0"
  alarm_actions       = [var.sns_arn]
  dimensions = {
    DBInstanceIdentifier = local.env
  }
}

resource "aws_cloudfront_origin_access_control" "oac" {
	name                              = "origin-access-control-${local.env}"
	description                       = "ab2d static website"
	origin_access_control_origin_type = "s3"
	signing_behavior                  = "always"
	signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_function" "redirects" {
  name    = "redesign-redirects"
  runtime = "cloudfront-js-2.0"
  comment = "Handle cool URIs and redirects for the redesign"
  code    = file("${path.module}/redirects-function.js")
}

resource "aws_cloudfront_distribution" "s3_distribution_not_prod" {
  count = local.env == "ab2d-east-impl" || local.env == "ab2d-dev" || local.env == "ab2d-sbx-sandbox" ? 1 : 0
  origin {
    domain_name              = "website-${local.env}.s3.us-east-1.amazonaws.com"
    origin_id                = "${local.env}-origin"
    origin_access_control_id = aws_cloudfront_origin_access_control.oac.id
  }
    enabled             = true
    comment                        = "Distribution for ${local.env} website"
    default_root_object            = "index.html"
    is_ipv6_enabled                = true
    price_class                    = "PriceClass_100"
    http_version                   = "http2and3"
    web_acl_id                     = data.aws_wafv2_web_acl.cms_waf_cdn.arn             
  restrictions {
    geo_restriction {
      restriction_type = "whitelist"
      locations        = ["US"]
    }
  }
  viewer_certificate {
    cloudfront_default_certificate = true
  }

  default_cache_behavior {
    # Use the CachingDisabled managed policy
    cache_policy_id        = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "${local.env}-origin"
    compress               = true
    viewer_protocol_policy = "redirect-to-https"
    function_association {
      event_type   = "viewer-request"
      function_arn = aws_cloudfront_function.redirects.arn
    }
  }

  custom_error_response {
    error_caching_min_ttl = 10
    error_code            = 403
    response_code         = 404
    response_page_path    = "/uswds-redesign/404.html"
  }
}

resource "aws_cloudfront_distribution" "s3_distribution_prod" {
  count = local.env == "ab2d-east-prod" ? 1 : 0
  aliases = ["ab2d.cms.gov"]
  origin {
    domain_name              = "website-${local.env}.s3.us-east-1.amazonaws.com"
    origin_id                = "${local.env}-origin"
    origin_access_control_id = aws_cloudfront_origin_access_control.oac.id
  }
    enabled             = true
    comment                        = "Distribution for ${local.env} website"
    default_root_object            = "index.html"
    is_ipv6_enabled                = true
    price_class                    = "PriceClass_100"
    http_version                   = "http2and3"
    web_acl_id                     = data.aws_wafv2_web_acl.cms_waf_cdn.arn             
  restrictions {
    geo_restriction {
      restriction_type = "whitelist"
      locations        = ["US"]
    }
  }
  viewer_certificate {
    acm_certificate_arn = data.aws_acm_certificate.cdn_ssl[count.index].arn
    cloudfront_default_certificate = false
    minimum_protocol_version = "TLSv1.2_2021"
    ssl_support_method = "sni-only"
  }

  default_cache_behavior {
    # Use the CachingOptimized managed policy
    cache_policy_id        = "658327ea-f89d-4fab-a63d-7e88639e58f6"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "${local.env}-origin"
    compress               = true
    viewer_protocol_policy = "redirect-to-https"

    function_association {
      event_type   = "viewer-request"
      function_arn = aws_cloudfront_function.redirects.arn
    }    
  }
}
