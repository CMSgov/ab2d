moved {
  from = aws_ecs_task_definition.worker
  to   = module.service.aws_ecs_task_definition.this
}

moved {
  from = aws_ecs_service.worker
  to   = module.service.aws_ecs_service.this
}
