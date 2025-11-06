variable "contracts_service_image_tag" {
  default     = null
  description = "Desired image tag for the contracts service stored in ECR"
  type        = string
}

variable "force_contracts_deployment" {
  default     = false
  description = "Override to force a contracts deployment. Contracts deployments are automatic when `var.contracts_service_image_tag` is specified."
  type        = bool
}

variable "events_service_image_tag" {
  default     = null
  description = "Desired image tag for the events service stored in ECR"
  type        = string
}

variable "force_events_deployment" {
  default     = false
  description = "Override to force a events deployment. Events deployments are automatic when `var.events_service_image_tag` is specified."
  type        = bool
}
