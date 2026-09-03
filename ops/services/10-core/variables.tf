variable "aurora_snapshot" {
  default     = null
  description = "Desired aurora snapshot on which the aurora cluser is based. Setting this is only meaningful on instantiation and is otherwise ignored."
  type        = string
}

variable "slack_alerts_enabled" {
  default     = null
  type        = bool
}
