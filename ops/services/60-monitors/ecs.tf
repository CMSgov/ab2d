###############################################################################
# ECS health monitors
#
# These are Datadog monitors, not CloudWatch alarms: 60-monitors is the Datadog terraservice, and
# Datadog's AWS ECS integration already reports desired/running/pending task counts per service
# tagged by {servicename}. That removes the cluster input CloudWatch requires, so one monitor covers
# every AB2D ECS service — api, contracts, events, worker — regardless of which cluster it lives in.
#
# Both monitors are app-agnostic. They belong upstream in
# cdap//terraform/modules/datadog_monitors/ecs.tf behind `monitor_config.ecs`; they live here only
# until that lands, at which point these two entries get deleted rather than rewritten (the names
# already match the module's convention).
###############################################################################

locals {
  ecs_monitor_scope = "application:${local.app},environment:${local.env}"

  # Datadog polls the ECS API rather than scraping it, so task counts land every few minutes with
  # gaps. That makes these sparse metrics: require_full_window must stay false, or the window is
  # never "full", the monitor never evaluates, and it sits in No Data forever (verified in dev —
  # with it on, the monitor resolved zero groups while an identical query with it off resolved all
  # five). The min() aggregator is what gives us "sustained", not the full window.
  ecs_monitors = [
    {
      name = "[${upper(local.env)}] [${local.app}] ECS — Running Tasks Below Desired"
      # Arithmetic across two metrics, so this has to be a query alert rather than a metric alert.
      type = "query alert"
      message = join(" ", [
        "ECS service {{servicename.name}} has been running fewer tasks than desired for 15 minutes.",
        "Tasks are dying or cannot be placed — services deploy with",
        "deployment_minimum_healthy_percent = 100, so a rolling deployment never takes the running",
        "count below desired. Check the service's stopped tasks in the ECS console for the",
        "stoppedReason and exit code.",
      ])
      query = "min(last_15m):avg:aws.ecs.service.desired{${local.ecs_monitor_scope}} by {servicename} - avg:aws.ecs.service.running{${local.ecs_monitor_scope}} by {servicename} > 0"
      thresholds = {
        critical = 0
      }
      # A service that stops reporting task counts altogether is the quietest failure of all, so
      # absent data pages rather than being ignored.
      on_missing_data     = "show_and_notify_no_data"
      require_full_window = false
      tags                = ["service:ecs"]
    },
    {
      name    = "[${upper(local.env)}] [${local.app}] ECS — Tasks Stuck Pending"
      type    = "query alert"
      message = "ECS service {{servicename.name}} has had tasks stuck in PENDING for 30 minutes. Tasks cannot be placed: check subnet IP capacity, and whether the task can pull its image and read its secrets."
      query   = "min(last_30m):avg:aws.ecs.service.pending{${local.ecs_monitor_scope}} by {servicename} > 0"
      thresholds = {
        critical = 0
      }
      # No-data here means the integration stopped reporting, which the monitor above already pages
      # on — no need to page twice.
      on_missing_data     = "default"
      require_full_window = false
      tags                = ["service:ecs"]
    },
  ]
}
