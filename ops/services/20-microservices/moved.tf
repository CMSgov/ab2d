moved {
  from = aws_ecs_task_definition.contracts
  to   = module.service.aws_ecs_task_definition.this
}

moved {
  from = aws_ecs_service.contracts
  to   = module.service.aws_ecs_service.this
}
