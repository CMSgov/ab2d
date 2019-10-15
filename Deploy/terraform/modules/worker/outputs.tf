output "aws_autoscaling_group_name" {
  value = aws_autoscaling_group.asg.name
}

output "aws_autoscaling_policy_percent_capacity_arn" {
  value = aws_autoscaling_policy.percent_capacity.arn
}

output "alb_target_group_arn_suffix" {
  value = aws_lb_target_group.worker.arn_suffix
}

output "alb_arn_suffix" {
  value = aws_lb.worker.arn_suffix
}