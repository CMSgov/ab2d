locals {
  ecs_monitor_scope = "application:${local.app},environment:${local.env}"

  ecs_monitors = [
    {
      name    = "[${upper(local.env)}] [${local.app}] ECS — Running Tasks Below Desired"
      type    = "query alert"
      message = "ECS service {{servicename.name}} has been running fewer tasks than it should for 15 minutes. Tasks are failing or cannot start. Check the service's stopped tasks in ECS for the reason and exit code."
      query   = "min(last_15m):avg:aws.ecs.service.desired{${local.ecs_monitor_scope}} by {servicename} - avg:aws.ecs.service.running{${local.ecs_monitor_scope}} by {servicename} > 0"
      thresholds = {
        critical = 0
      }
      on_missing_data = "show_and_notify_no_data"
      tags            = ["service:ecs"]
    },
    {
      name    = "[${upper(local.env)}] [${local.app}] ECS — Tasks Stuck Pending"
      type    = "query alert"
      message = "ECS service {{servicename.name}} has had tasks stuck in PENDING for 30 minutes, so they cannot start. Check that the subnets have free IP addresses, and that the task can pull its image and read its secrets."
      query   = "min(last_30m):avg:aws.ecs.service.pending{${local.ecs_monitor_scope}} by {servicename} > 0"
      thresholds = {
        critical = 0
      }
      tags = ["service:ecs"]
    },
  ]
}
