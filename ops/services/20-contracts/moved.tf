moved {
  from = aws_ecs_task_definition.contracts
  to   = module.contracts_service.aws_ecs_task_definition.this
}

moved {
  from = aws_ecs_service.contracts
  to   = module.contracts_service.aws_ecs_service.this
}
