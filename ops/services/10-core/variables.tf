variable "aurora_snapshot" {
  default     = null
  description = "Desired aurora snapshot on which the aurora cluser is based. Setting this is only meaningful on instantiation and is otherwise ignored."
  type        = string
}

variable "slack_alerts_enabled" {
  description = "Subscribe the CloudWatch alarm topic to the CDAP alarm-to-slack queue. Defaults to prod only."
  default     = null
  type        = bool
}
