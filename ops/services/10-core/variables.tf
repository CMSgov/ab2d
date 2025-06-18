variable "snapshot" {
  default     = null
  description = "Desired snapshot from which the RDS instance is created. Setting this is only meaningful on instantiation and is otherwise ignored."
  type        = string
}
