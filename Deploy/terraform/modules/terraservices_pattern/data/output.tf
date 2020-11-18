output "database_instance_identifier" {
  value = aws_db_instance.db.identifier
}

output "database_parameter_group_name" {
  value = aws_db_parameter_group.parameter_group.name
}

output "database_security_group_id" {
  value = aws_security_group.sg_database.id
}

output "database_subnet_group_name" {
  value = aws_db_subnet_group.subnet_group.name
}
