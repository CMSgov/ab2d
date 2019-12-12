output "aws_autoscaling_group_name" {
  value = aws_autoscaling_group.asg.name
}

output "aws_autoscaling_policy_percent_capacity_arn" {
  value = aws_autoscaling_policy.percent_capacity.arn
}

output "ecs_cluster_id" {
  value = aws_ecs_cluster.ab2d_worker.id
}