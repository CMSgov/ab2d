output "ab2d_cloud_watch_logs_policy_name" {
  value = aws_iam_policy.cloud_watch_logs_policy.name
}

output "ab2d_instance_profile" {
  value = aws_iam_instance_profile.ab2d_instance_profile.name
}

output "ab2d_instance_role_name" {
  value = aws_iam_role.ab2d_instance_role.name
}

output "ab2d_packer_policy_name" {
  value = aws_iam_policy.packer_policy.name
}

output "ab2d_s3_access_policy_name" {
  value = aws_iam_policy.s3_access_policy.name
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
