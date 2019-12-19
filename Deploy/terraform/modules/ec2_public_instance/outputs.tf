output "test_controller_sec_group_id" {
  value = aws_security_group.test_controller.id
}

output "test_controller_public_ip" {
  value = aws_eip.test_controller.public_ip
}

output "test_controller_id" {
  value = aws_instance.test_controller.id
}
