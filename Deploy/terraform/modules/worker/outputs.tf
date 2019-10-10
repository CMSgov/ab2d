output "deployment_controller_sec_group_id" {
  value = aws_security_group.deployment_controller.id
}

output "deployment_controller_public_ip" {
  value = aws_eip.deployment_controller.public_ip
}

output "aws_autoscaling_group_name" {
  value = aws_autoscaling_group.asg.name
}

output "application_security_group_id" {
  value = aws_security_group.api.id
}

output "deployment_controller_id" {
  value = aws_instance.deployment_controller.id
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

output "alb_arn_suffix" {
  value = aws_lb.api.arn_suffix
}

output "userdata" {
  value = aws_launch_configuration.launch_config.user_data
}

output "ecs_cluster_id" {
  value = aws_ecs_cluster.ab2d.id
}

output "launch_config_name" {
  value = aws_launch_configuration.launch_config.name
}
