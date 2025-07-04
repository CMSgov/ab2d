# AB2D Insights Root Module

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
| <a name="input_parent_env"></a> [parent\_env](#input\_parent\_env) | The parent environment of the current solution. Will correspond with `terraform.workspace`".<br/>Necessary on `tofu init` and `tofu workspace select` \_only\_. In all other situations, parent env<br/>will be divined from `terraform.workspace`. | `string` | `null` | no |
| <a name="input_region"></a> [region](#input\_region) | n/a | `string` | `"us-east-1"` | no |
| <a name="input_secondary_region"></a> [secondary\_region](#input\_secondary\_region) | n/a | `string` | `"us-west-2"` | no |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_platform"></a> [platform](#module\_platform) | git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform | PLT-1099 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_iam_role.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_quicksight_analysis.a](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_analysis) | resource |
| [aws_quicksight_dashboard.a](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_dashboard) | resource |
| [aws_quicksight_data_set.ab2d_statistics](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_set.benes_searched](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_set.contracts](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_set.contracts_one_job_minimum](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_set.coverage_counts](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_set.eob_search_summaries_1](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_set.eob_search_summaries_2](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_set.eob_search_summaries_event](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_set.job_view](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_set.total_benes_pulled_per_week_2_0](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_set.uptime](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_set) | resource |
| [aws_quicksight_data_source.rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_data_source) | resource |
| [aws_quicksight_template.a](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_template) | resource |
| [aws_quicksight_vpc_connection.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/quicksight_vpc_connection) | resource |
| [aws_security_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group_rule.db_ingress](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group_rule) | resource |
| [aws_security_group_rule.quicksight_egress](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group_rule) | resource |
| [aws_db_instance.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/db_instance) | data source |
| [aws_security_group.db](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

No outputs.
<!-- END_TF_DOCS -->
