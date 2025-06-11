variable "launch_template_block_device_mappings" {
  default = {
    delete_on_termination = true
    device_name           = "/dev/xvda"
    encrypted             = true
    iops                  = 3000
    throughput            = 250
    volume_size           = 100
    volume_type           = "gp3"
  }
  description = "ECS Container Host block device map"
  type        = map(any)
}

variable "override_task_definition_arn" {
  default     = null
  description = "Use to override the task definition managed by this solution"
  type        = string
}

variable "worker_service_image_tag" {
  default     = null
  description = "Desired image tag for the worker service stored in ECR"
  type        = string
}

variable "force_worker_deployment" {
  default     = false
  description = "Override to force a worker deployment. Worker deployments are automatic when `var.worker_service_image_tag` is specified."
  type        = bool
}
