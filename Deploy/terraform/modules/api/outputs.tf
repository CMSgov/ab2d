output "aws_autoscaling_group_name" {
  value = aws_autoscaling_group.asg.name
}

output "application_security_group_id" {
  value = aws_security_group.api.id
}

output "aws_autoscaling_policy_percent_capacity_arn" {
  value = aws_autoscaling_policy.percent_capacity.arn
}

output "aws_elastic_load_balancer_name" {
  value = aws_lb.api.name
}

output "alb_target_group_arn_suffix" {
  value = aws_lb_target_group.api.arn_suffix
}

output "alb_arn" {
  value = aws_lb.api.arn
}

output "alb_arn_suffix" {
  value = aws_lb.api.arn_suffix
}

output "userdata" {
  value = aws_launch_configuration.launch_config.user_data
}

output "ecs_cluster_id" {
  value = aws_ecs_cluster.ab2d_api.id
}

output "launch_config_name" {
  value = aws_launch_configuration.launch_config.name
}
