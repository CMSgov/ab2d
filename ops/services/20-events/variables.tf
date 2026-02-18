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
