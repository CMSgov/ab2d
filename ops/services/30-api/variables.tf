variable "override_task_definition_arn" {
  default     = null
  description = "Use to override the task definition managed by this solution"
  type        = string
}

variable "api_service_image_tag" {
  default     = null
  description = "Desired image tag for the api service stored in ECR"
  type        = string
}

variable "force_api_deployment" {
  default     = false
  description = "Override to force a api deployment. Api deployments are automatic when `var.api_service_image_tag` is specified."
  type        = bool
}

# temporary env variable
variable "env" {
  description = "The environment being deployed (dev, test, sandbox, prod)"
  type        = string
}
