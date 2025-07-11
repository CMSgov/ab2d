<!-- BEGIN_TF_DOCS -->
<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Providers

| Name | Version |
|------|---------|
| <a name="provider_aws"></a> [aws](#provider\_aws) | n/a |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Requirements

No requirements.

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_allocated_storage"></a> [allocated\_storage](#input\_allocated\_storage) | n/a | `number` | n/a | yes |
| <a name="input_aurora_instance_class"></a> [aurora\_instance\_class](#input\_aurora\_instance\_class) | Aurora cluster instance class | `string` | n/a | yes |
| <a name="input_backup_window"></a> [backup\_window](#input\_backup\_window) | Daily time range during which automated backups are created if automated backups are enabled in UTC, e.g. `04:00-09:00` | `string` | n/a | yes |
| <a name="input_instance_class"></a> [instance\_class](#input\_instance\_class) | n/a | `string` | n/a | yes |
| <a name="input_maintenance_window"></a> [maintenance\_window](#input\_maintenance\_window) | Weekly time range during which system maintenance can occur in UTC, e.g. `wed:04:00-wed:04:30` | `string` | n/a | yes |
| <a name="input_password"></a> [password](#input\_password) | The database's primary/master credentials password | `string` | n/a | yes |
| <a name="input_platform"></a> [platform](#input\_platform) | Object that describes standardized platform values. | `any` | n/a | yes |
| <a name="input_username"></a> [username](#input\_username) | The database's primary/master credentials username | `string` | n/a | yes |
| <a name="input_aurora_cluster_instance_parameters"></a> [aurora\_cluster\_instance\_parameters](#input\_aurora\_cluster\_instance\_parameters) | A list of objects containing the values for apply\_method, name, and value that corresponds to the instance-level prameters. | <pre>list(object({<br/>    apply_method = string<br/>    name         = string<br/>    value        = any<br/>  }))</pre> | `[]` | no |
| <a name="input_aurora_cluster_parameters"></a> [aurora\_cluster\_parameters](#input\_aurora\_cluster\_parameters) | A list of objects containing the values for apply\_method, name, and value that corresponds to the cluster-level prameters. | <pre>list(object({<br/>    apply_method = string<br/>    name         = string<br/>    value        = any<br/>  }))</pre> | `[]` | no |
| <a name="input_aurora_snapshot"></a> [aurora\_snapshot](#input\_aurora\_snapshot) | Specifies whether or not to create this cluster from a snapshot, using snapshot name or ARN. | `string` | `null` | no |
| <a name="input_aurora_storage_type"></a> [aurora\_storage\_type](#input\_aurora\_storage\_type) | n/a | `string` | `"aurora-iopt1"` | no |
| <a name="input_backup_retention_period"></a> [backup\_retention\_period](#input\_backup\_retention\_period) | Days to retain backups for. | `number` | `1` | no |
| <a name="input_create_aurora_cluster"></a> [create\_aurora\_cluster](#input\_create\_aurora\_cluster) | When true, an aurora cluster will be created. | `bool` | `false` | no |
| <a name="input_create_rds_db_instance"></a> [create\_rds\_db\_instance](#input\_create\_rds\_db\_instance) | n/a | `bool` | `false` | no |
| <a name="input_deletion_protection"></a> [deletion\_protection](#input\_deletion\_protection) | If the DB cluster should have deletion protection enabled. | `bool` | `true` | no |
| <a name="input_engine_version"></a> [engine\_version](#input\_engine\_version) | Selected engine version for either RDS DB Instance or RDS Aurora DB Cluster. | `string` | `"16.8"` | no |
| <a name="input_iops"></a> [iops](#input\_iops) | n/a | `number` | `null` | no |
| <a name="input_kms_key_override"></a> [kms\_key\_override](#input\_kms\_key\_override) | Override to the platform-managed KMS key | `string` | `null` | no |
| <a name="input_monitoring_interval"></a> [monitoring\_interval](#input\_monitoring\_interval) | [monitoring\_interval](https://registry.terraform.io/providers/hashicorp/aws/5.100.0/docs/resources/rds_cluster#monitoring_interval-1). Interval, in seconds, in seconds, between points when Enhanced Monitoring metrics are collected for the DB cluster. | `number` | `15` | no |
| <a name="input_monitoring_role_arn"></a> [monitoring\_role\_arn](#input\_monitoring\_role\_arn) | ARN for the IAM role that permits RDS to send enhanced monitoring metrics to CloudWatch Logs. | `string` | `null` | no |
| <a name="input_multi_az"></a> [multi\_az](#input\_multi\_az) | n/a | `bool` | `false` | no |
| <a name="input_snapshot"></a> [snapshot](#input\_snapshot) | For use in restoring a snapshot to a traditional RDS DB Instance. | `string` | `null` | no |
| <a name="input_storage_type"></a> [storage\_type](#input\_storage\_type) | n/a | `string` | `null` | no |
| <a name="input_vpc_security_group_ids"></a> [vpc\_security\_group\_ids](#input\_vpc\_security\_group\_ids) | Additional security group ids for attachment to the data base security group. | `list(string)` | `[]` | no |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Modules

No modules.

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_db_instance.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_instance) | resource |
| [aws_db_parameter_group.aurora](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_parameter_group) | resource |
| [aws_db_parameter_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_parameter_group) | resource |
| [aws_db_subnet_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_subnet_group) | resource |
| [aws_rds_cluster.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/rds_cluster) | resource |
| [aws_rds_cluster_instance.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/rds_cluster_instance) | resource |
| [aws_rds_cluster_parameter_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/rds_cluster_parameter_group) | resource |
| [aws_security_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_vpc_security_group_egress_rule.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_egress_rule) | resource |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

| Name | Description |
|------|-------------|
| <a name="output_aurora_cluster"></a> [aurora\_cluster](#output\_aurora\_cluster) | n/a |
| <a name="output_aurora_instance"></a> [aurora\_instance](#output\_aurora\_instance) | n/a |
| <a name="output_instance"></a> [instance](#output\_instance) | n/a |
| <a name="output_security_group"></a> [security\_group](#output\_security\_group) | n/a |
<!-- END_TF_DOCS -->