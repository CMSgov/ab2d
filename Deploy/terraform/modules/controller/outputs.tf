output "deployment_controller_sec_group_id" {
  value = aws_security_group.deployment_controller.id
}

output "deployment_controller_private_ip" {
  value = aws_eip.deployment_controller.private_ip
}

output "deployment_controller_id" {
  value = aws_instance.deployment_controller.id
}
