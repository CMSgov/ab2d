variable "launch_template_block_device_mappings" {
  default = {
    device_name           = "/dev/xvda"
    iops                  = 3000
    throughput            = 250
    volume_size           = 100
    volume_type           = "gp3"
    delete_on_termination = true
    encrypted             = true
  }
  description = "ECS Container Host block device map"
  type        = map(any)
}

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
