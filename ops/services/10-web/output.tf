output "domain_name" {
  description = "Default Domain Name for CloudFront Distribution"
  value       = aws_cloudfront_distribution.this.domain_name
}
