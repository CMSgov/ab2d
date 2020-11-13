output "ab2d_packer_policy_name" {
  value = aws_iam_policy.packer_policy.name
}

output "ab2d_s3_access_policy_name" {
  value = aws_iam_policy.s3_access_policy.name
}

output "ab2d_cloud_watch_logs_policy_name" {
  value = aws_iam_policy.cloud_watch_logs_policy.name
}

output "ab2d_instance_role_name" {
  value = aws_iam_role.ab2d_instance_role.name
}

output "ab2d_instance_profile" {
  value = aws_iam_instance_profile.ab2d_instance_profile.name
}
