resource "aws_autoscaling_schedule" "night" {
  scheduled_action_name  = "night"
  min_size               = 0
  max_size               = 0
  desired_capacity       = 0
  recurrence             = "00 05 * * 1-5" #Mon-Fri at 12:00 AM Midnight EST
  autoscaling_group_name = module.api.aws_autoscaling_group_name
}

resource "aws_autoscaling_schedule" "morning" {
  scheduled_action_name  = "morning"
  min_size               = 2
  max_size               = 2
  desired_capacity       = 2
  recurrence             = "00 12 * * 1-5" #Mon-Fri at 7AM EST
  autoscaling_group_name = module.api.aws_autoscaling_group_name
}

