resource "aws_kinesis_firehose_delivery_stream" "main" {
  for_each = toset(var.kinesis_firehose_delivery_streams)
  name     = each.value
  
  tags = {
    Environment = "${var.env}"
    managed_by  = "Terraform"
  }
  
  destination = "extended_s3"

  # Encrypt while processing
  server_side_encryption {
    enabled = true
  } 

  extended_s3_configuration {
    role_arn            = "arn:aws:iam::${var.aws_account_number}:role/delegatedadmin/developer/${var.kinesis_firehose_role}"
    bucket_arn          = "arn:aws:s3:::${var.kinesis_firehose_bucket}"
    kms_key_arn         = "${var.kinesis_firehose_kms_key_arn}" # Encrypt on delivery
    buffer_size         = 5
    buffer_interval     = 60
    compression_format  = "GZIP"
    prefix              = "databases/ab2d/${each.value}/dt=!{timestamp:yyyy-MM-dd}/"
    error_output_prefix = "databases/ab2d/${each.value}_errors/!{firehose:error-output-type}/!{timestamp:yyyy-MM-dd}/"

    cloudwatch_logging_options {
      enabled         = true
      log_group_name  = "/aws/kinesisfirehose/${each.value}"
      log_stream_name = "S3Delivery"
    }
  }
}
