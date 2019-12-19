output "deployment_controller_sec_group_id" {
  value = aws_security_group.deployment_controller.id
}

output "deployment_controller_public_ip" {
  value = aws_eip.deployment_controller.public_ip
}

output "deployment_controller_id" {
  value = aws_instance.deployment_controller.id
}
