output "test_node_sec_group_id" {
  value = aws_security_group.test_node.id
}

output "test_node_id" {
  value = aws_instance.test_node.id
}
