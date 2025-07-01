output "instance" {
  value = aws_db_instance.this
}

output "sg" {
  value = aws_security_group.this
}

output "aurora_cluster" {
  value = aws_rds_cluster.aurora
}

output "aurora_instance" {
  value = aws_rds_cluster_instance.aurora_instance
}

output "aurora_endpoint" {
  value = aws_rds_cluster.aurora.endpoint
}
