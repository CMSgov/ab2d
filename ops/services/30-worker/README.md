# Worker Child Module

This module is responsible for creating the EC2-backed ECS Cluster and related resources for hosting AB2D's [worker service ](https://github.com/CMSgov/ab2d/tree/main/worker).

## Dependencies
- [ab2d ecr terraservice](https://github.com/CMSgov/ab2d-ops/tree/main/terraform/services/ecr) (greenfield only)
- [worker container workload](https://github.com/CMSgov/ab2d/tree/main/worker)
- [ab2d "app" AMI](https://github.com/CMSgov/ab2d-ops/blob/main/automation/packer/app/app.json)
- [cdap api-rds terraservice](https://github.com/CMSgov/cdap/tree/main/terraform/services/api-rds)
- [ab2d core](https://github.com/CMSgov/ab2d-ops/tree/main/terraform/modules/core)
- [ab2d microservices](https://github.com/CMSgov/ab2d-ops/tree/main/terraform/modules/microservices)

<!-- BEGIN_TF_DOCS -->
<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Providers

| Name | Version |
|------|---------|
| <a name="provider_aws"></a> [aws](#provider\_aws) | 5.100.0 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~> 5 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_force_worker_deployment"></a> [force\_worker\_deployment](#input\_force\_worker\_deployment) | Override to force a worker deployment. Worker deployments are automatic when `var.worker_service_image_tag` is specified. | `bool` | `false` | no |
| <a name="input_override_task_definition_arn"></a> [override\_task\_definition\_arn](#input\_override\_task\_definition\_arn) | Use to override the task definition managed by this solution | `string` | `null` | no |
| <a name="input_parent_env"></a> [parent\_env](#input\_parent\_env) | The parent environment of the current solution. Will correspond with `terraform.workspace`".<br/>Necessary on `tofu init` and `tofu workspace select` \_only\_. In all other situations, parent env<br/>will be divined from `terraform.workspace`. | `string` | `null` | no |
| <a name="input_region"></a> [region](#input\_region) | n/a | `string` | `"us-east-1"` | no |
| <a name="input_secondary_region"></a> [secondary\_region](#input\_secondary\_region) | n/a | `string` | `"us-west-2"` | no |
| <a name="input_worker_service_image_tag"></a> [worker\_service\_image\_tag](#input\_worker\_service\_image\_tag) | Desired image tag for the worker service stored in ECR | `string` | `null` | no |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_data_db_writer_instance"></a> [data\_db\_writer\_instance](#module\_data\_db\_writer\_instance) | github.com/CMSgov/beneficiary-fhir-data//ops/terraform-modules/general/data-db-writer-instance | 2.211.0 |
| <a name="module_platform"></a> [platform](#module\_platform) | github.com/CMSgov/cdap//terraform/modules/platform | ff2ef539fb06f2c98f0e3ce0c8f922bdacb96d66 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_ecs_cluster.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ecs_cluster) | resource |
| [aws_ecs_service.worker](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ecs_service) | resource |
| [aws_ecs_task_definition.worker](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ecs_task_definition) | resource |
| [aws_security_group_rule.db_access_worker](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group_rule) | resource |
| [aws_security_group_rule.efs_ingress](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group_rule) | resource |
| [aws_security_group_rule.egress_worker](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group_rule) | resource |
| [aws_ecr_image.worker](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_image) | data source |
| [aws_efs_access_point.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/efs_access_point) | data source |
| [aws_efs_file_system.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/efs_file_system) | data source |
| [aws_iam_role.worker](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_role) | data source |
| [aws_rds_cluster.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/rds_cluster) | data source |
| [aws_security_group.db](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.efs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.worker](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_sqs_queue.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sqs_queue) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

No outputs.
<!-- END_TF_DOCS -->
