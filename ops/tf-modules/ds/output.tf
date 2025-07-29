output "instance" {
  deprecated = "This will no longer be supported once APIs have migrated to aurora."
  value      = var.create_rds_db_instance ? aws_db_instance.this[0] : null
}

output "security_group" {
  value = aws_security_group.this
}

output "aurora_cluster" {
  value = var.create_aurora_cluster ? aws_rds_cluster.this[0] : null
}

output "aurora_instance" {
  value = var.create_aurora_cluster ? aws_rds_cluster_instance.this[0] : null
}
