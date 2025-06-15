output "domain_name" {
  description = "Default Domain Name for CloudFront Distribution"
  value       = aws_cloudfront_distribution.this.domain_name
}

output "bucket_id" {
  description = "Default Domain Name for CloudFront Distribution"
  value       = aws_s3_bucket.this.id
}

output "distribution_id" {
  description = "Default Domain Name for CloudFront Distribution"
  value       = aws_cloudfront_distribution.this.id
}
