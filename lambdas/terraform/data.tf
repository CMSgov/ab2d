data aws_lambda_invocation "update_database_schema" {
  depends_on    = [aws_lambda_function.database_management]
  function_name = aws_lambda_function.database_management.function_name
  input         = <<JSON
  {
  }
  JSON
}