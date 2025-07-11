variable "snapshot" {
  default     = null
  description = "Desired snapshot from which the RDS instance is created. Setting this is only meaningful on instantiation and is otherwise ignored."
  type        = string
}

variable "aurora_snapshot" {
  default     = null
  description = "Desired aurora snapshot from which the aurora cluser is created. Setting this is only meaningful on instantiation and is otherwise ignored."
  type        = string
}
