output "result_entry" {
  value = jsondecode(data.aws_lambda_invocation.update_database_schema.result)
}
