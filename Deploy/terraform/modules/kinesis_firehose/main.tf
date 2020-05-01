resource "aws_kinesis_firehose_delivery_stream" "main" {
  name                  = "bfd-insights-ab2d-apirequestevent"
  # tags                  = "test"
  destination           = "extended_s3"

  # Encrypt while processing
  server_side_encryption {
    enabled = true
  } 

  extended_s3_configuration {
    role_arn            = "arn:aws:iam::777200079629:role/Ab2dBfdInsightsRole"
    bucket_arn          = "arn:aws:s3:::bfd-insights-ab2d-577373831711"
    kms_key_arn         = "arn:aws:kms:us-east-1:577373831711:key/97973f21-cdc5-421e-83a8-8545b007999f" # Encrypt on delivery
    buffer_size         = 5
    buffer_interval     = 60
    compression_format  = "GZIP"
    prefix              = "databases/ab2d/bfd_insights_ab2d_apirequestevent/dt=!{timestamp:yyyy-MM-dd}/"
    error_output_prefix = "databases/ab2d/bfd_insights_ab2d_apirequestevent_errors/!{firehose:error-output-type}/!{timestamp:yyyy-MM-dd}/"

    cloudwatch_logging_options {
      enabled         = true
      log_group_name  = "/aws/kinesisfirehose/bfd-insights-ab2d"
      log_stream_name = "S3Delivery"
    }
  }
}
