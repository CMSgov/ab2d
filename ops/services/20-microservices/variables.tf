variable "contracts_service_image_tag" {
  default     = null
  description = "Desired image tag for the contracts service stored in ECR"
  type        = string
}

variable "events_service_image_tag" {
  default     = null
  description = "Desired image tag for the events service stored in ECR"
  type        = string
}

variable "properties_service_image_tag" {
  default     = null
  description = "Desired image tag for the properties service stored in ECR"
  type        = string
}

