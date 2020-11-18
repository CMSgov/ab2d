output "ab2d_cloud_watch_logs_policy_name" {
  value = aws_iam_policy.cloud_watch_logs_policy.name
}

output "ab2d_instance_profile" {
  value = aws_iam_instance_profile.ab2d_instance_profile.name
}

output "ab2d_instance_role_name" {
  value = aws_iam_role.ab2d_instance_role.name
}

output "ab2d_kms_policy_name" {
  value = aws_iam_policy.kms_policy.name
}

output "ab2d_packer_policy_name" {
  value = aws_iam_policy.packer_policy.name
}

output "ab2d_s3_access_policy_name" {
  value = aws_iam_policy.s3_access_policy.name
}

output "efs_dns_name" {
  value = aws_efs_file_system.efs.dns_name
}

output "efs_id" {
  value = aws_efs_file_system.efs.id
}

output "efs_sg_id" {
  value = aws_security_group.efs.id
}

output "main_bucket_name" {
  value = aws_s3_bucket.main_bucket.id
}

output "main_kms_key_alias" {
  value = aws_kms_alias.main_kms_key_alias.name
}

output "main_kms_key_arn" {
  value = aws_kms_key.main_kms_key.arn
}

output "main_kms_key_id" {
  value = aws_kms_key.main_kms_key.id
}

output "main_log_bucket_name" {
  value = aws_s3_bucket.main_log_bucket.id
}

output "private_subnet_a_id" {
  value = data.aws_subnet.private_subnet_a.id
}

output "private_subnet_b_id" {
  value = data.aws_subnet.private_subnet_b.id
}

output "vpc_id" {
  value = data.aws_vpc.target_vpc.id
}
