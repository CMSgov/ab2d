# Output vars
output "aws_security_group_sg_database_id" {
  value = aws_security_group.sg_database.id
}

output "rds_hostname" {
  value = aws_db_instance.db.address
}

output "rds_port" {
  value = aws_db_instance.db.port
}
