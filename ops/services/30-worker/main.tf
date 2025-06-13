terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5"
    }
  }
}

module "platform" {
  source    = "git::https://github.com/CMSgov/ab2d-bcda-dpc-platform.git//terraform/modules/platform?ref=PLT-1099"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app          = local.app
  env          = local.env
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/30-worker"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "worker"

  ssm_root_map = {
    common        = "/ab2d/${local.parent_env}/common"
    core          = "/ab2d/${local.parent_env}/core"
    microservices = "/ab2d/${local.parent_env}/microservices"
    worker        = "/ab2d/${local.parent_env}/worker"
    accounts      = "/ab2d/mgmt/aws-account-numbers"
  }

  bfd_insights       = "none"
  private_subnet_ids = keys(module.platform.private_subnets)

  #TODO in honor of Ben "Been Jammin'" Hesford
  benv = lookup({
    "dev"     = "ab2d-dev"
    "test"    = "ab2d-east-impl"
    "prod"    = "ab2d-east-prod"
    "sandbox" = "ab2d-sbx-sandbox"
  }, local.parent_env, local.env)

  region_name = module.platform.primary_region.name
  vpc_id      = module.platform.vpc_id

  bfd_url = lookup({
    prod = "https://prod.fhir.bfd.cmscloud.local"
  }, local.parent_env, "https://prod-sbx.fhir.bfd.cmscloud.local")

  ab2d_efs_mount            = "/mnt/efs"
  bfd_keystore_location     = module.platform.ssm.worker.bfd_keystore_location.value
  bfd_keystore_password_arn = module.platform.ssm.worker.bfd_keystore_password.arn
  ami_id                    = data.aws_ami.ab2d_ami.id

  ec2_instance_type_worker   = module.platform.parent_env == "prod" ? "c6a.12xlarge" : "m6a.xlarge"
  ecs_task_def_cpu_worker    = module.platform.parent_env == "prod" ? 49152 : 4096
  ecs_task_def_memory_worker = module.platform.parent_env == "prod" ? 88473 : 14745
  gold_disk_name             = data.aws_ami.cms_gold.name
  image_version              = "" #FIXME aws_ami cms or ab2d?
  max_concurrent_eob_jobs    = "2"
  ssh_key_name               = "burldawg"
  worker_desired_instances   = module.platform.parent_env == "prod" ? 2 : 1
  worker_min_instances       = module.platform.parent_env == "prod" ? 2 : 1
  worker_max_instances       = module.platform.parent_env == "prod" ? 4 : 2

  db_name_arn                 = module.platform.ssm.core.database_name.arn
  db_password_arn             = module.platform.ssm.core.database_password.arn
  db_username_arn             = module.platform.ssm.core.database_user.arn
  microservices_url           = module.platform.ssm.microservices.url.value
  new_relic_app_name          = module.platform.ssm.common.new_relic_app_name.value
  new_relic_license_key_arn   = module.platform.ssm.common.new_relic_license_key.arn
  slack_alert_webhooks_arn    = module.platform.ssm.common.slack_alert_webhooks.arn
  slack_trace_webhooks_arn    = module.platform.ssm.common.slack_trace_webhooks.arn
  aws_account_cms_gold_images = module.platform.ssm.accounts.cms-gold-images.value

  additional_asg_tags = {
    Name             = "${local.service_prefix}-worker"
    stack            = local.env
    purpose          = "ECS container instance"
    sensitivity      = "Public"
    os_license       = "Amazon Linux 2023"
    gold_disk_name   = local.gold_disk_name
    image_version    = local.image_version
    AmazonECSManaged = true
  }

  # Use the provided image tag or get the first, human-readable image tag, favoring a tag with 'latest' in its name if it should exist.
  worker_image_repo = split("@", data.aws_ecr_image.worker.image_uri)[0]
  worker_image_tag  = coalesce(var.worker_service_image_tag, flatten([[for t in data.aws_ecr_image.worker.image_tags : t if strcontains(t, "latest")], data.aws_ecr_image.worker.image_tags])[0])
  worker_image_uri  = "${local.worker_image_repo}:${local.worker_image_tag}"
}

resource "aws_security_group_rule" "egress_worker" {
  type              = "egress"
  description       = "Allow all egress"
  from_port         = "0"
  to_port           = "0"
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = data.aws_security_group.worker.id
}

resource "aws_security_group_rule" "db_access_worker" {
  type                     = "ingress"
  description              = "${local.service_prefix} worker connections"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = data.aws_security_group.db.id
}

resource "aws_security_group_rule" "efs_ingress" {
  type                     = "ingress"
  description              = "NFS"
  from_port                = 2049
  to_port                  = 2049
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = data.aws_security_group.efs.id
}

resource "aws_ecs_cluster" "this" {
  name = "${local.service_prefix}-worker"
}

resource "aws_ecs_capacity_provider" "this" {
  name = aws_ecs_cluster.this.name

  auto_scaling_group_provider {
    auto_scaling_group_arn         = aws_autoscaling_group.this.arn
    managed_termination_protection = "ENABLED"
  }
}

resource "aws_ecs_cluster_capacity_providers" "this" {
  cluster_name       = aws_ecs_cluster.this.name
  capacity_providers = [aws_ecs_capacity_provider.this.name]
}

data "aws_sqs_queue" "events" {
  name = "${local.service_prefix}-events"
}

resource "aws_ecs_task_definition" "worker" {
  family = "${local.service_prefix}-worker"
  volume {
    name      = "efs"
    host_path = local.ab2d_efs_mount
  }
  container_definitions = nonsensitive(jsonencode([{
    name : local.service,
    image : local.worker_image_uri,
    essential : true,
    cpu : local.ecs_task_def_cpu_worker,
    memory : local.ecs_task_def_memory_worker,
    mountPoints : [
      {
        containerPath : local.ab2d_efs_mount,
        sourceVolume : "efs"
      }
    ],
    secrets : [
      { name : "AB2D_BFD_KEYSTORE_PASSWORD", valueFrom : local.bfd_keystore_password_arn },
      { name : "AB2D_DB_DATABASE", valueFrom : local.db_name_arn },
      { name : "AB2D_DB_PASSWORD", valueFrom : local.db_password_arn },
      { name : "AB2D_DB_USER", valueFrom : local.db_username_arn },
      { name : "AB2D_SLACK_ALERT_WEBHOOKS", valueFrom : local.slack_alert_webhooks_arn }, #FIXME: Is this even used?
      { name : "AB2D_SLACK_TRACE_WEBHOOKS", valueFrom : local.slack_trace_webhooks_arn }, #FIXME: Is this even used?
      { name : "NEW_RELIC_LICENSE_KEY", valueFrom : local.new_relic_license_key_arn }     #FIXME: Is this even used?
    ]
    environment : [
      { name : "AB2D_BFD_INSIGHTS", value : local.bfd_insights }, #FIXME: Is this even used?
      { name : "AB2D_BFD_KEYSTORE_LOCATION", value : local.bfd_keystore_location },
      { name : "AB2D_BFD_URL", value : local.bfd_url },
      { name : "AB2D_DB_HOST", value : data.aws_db_instance.this.address },
      { name : "AB2D_DB_PORT", value : "5432" },
      { name : "AB2D_DB_SSL_MODE", value : "require" },
      { name : "AB2D_EFS_MOUNT", value : local.ab2d_efs_mount },
      { name : "AB2D_EXECUTION_ENV", value : local.benv },
      { name : "AB2D_JOB_POOL_CORE_SIZE", value : local.max_concurrent_eob_jobs },
      { name : "AB2D_JOB_POOL_MAX_SIZE", value : local.max_concurrent_eob_jobs },
      { name : "AWS_SQS_FEATURE_FLAG", value : "true" }, #FIXME: Is this even used?
      { name : "AWS_SQS_URL", value : data.aws_sqs_queue.events.url },
      { name : "CONTRACTS_SERVICE_FEATURE_FLAG", value : "true" }, #FIXME: Is this even used?
      { name : "IMAGE_VERSION", value : local.worker_image_tag },
      { name : "NEW_RELIC_APP_NAME", value : local.new_relic_app_name },
      { name : "PROPERTIES_SERVICE_FEATURE_FLAG", value : "true" }, #FIXME: Is this even used?
      { name : "PROPERTIES_SERVICE_URL", value : local.microservices_url },
    ],
    logConfiguration : {
      logDriver : "syslog"
    },
    healthCheck : null
  }]))

  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"
  execution_role_arn       = data.aws_iam_role.worker.arn
}

resource "aws_ecs_service" "worker" {
  name                               = "${local.service_prefix}-worker"
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = coalesce(var.override_task_definition_arn, aws_ecs_task_definition.worker.arn)
  launch_type                        = "EC2"
  scheduling_strategy                = "DAEMON"
  force_new_deployment               = anytrue([var.force_worker_deployment, var.worker_service_image_tag != null])
  deployment_minimum_healthy_percent = 100
}

resource "aws_launch_template" "this" {
  name          = "${local.service_prefix}-worker"
  image_id      = local.ami_id
  instance_type = local.ec2_instance_type_worker
  key_name      = local.ssh_key_name

  iam_instance_profile {
    name = aws_iam_instance_profile.worker_profile.name
  }

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  monitoring {
    enabled = true
  }

  user_data = base64encode(
    templatefile(
      "${path.module}/templates/userdata.tpl",
      {
        aws_region             = local.region_name
        cluster_name           = "${local.service_prefix}-worker"
        efs_id                 = data.aws_efs_file_system.this.file_system_id
        env                    = local.env
        bucket_name            = module.platform.ssm.core.main-bucket-name.value
        keystore_dir           = module.platform.ssm.worker.bfd_keystore_location.value
        bfd_keystore_file_name = "ab2d_${local.env}_keystore"
        accesspoint            = data.aws_efs_access_point.this.id
      }
    )
  )

  vpc_security_group_ids = [
    data.aws_security_group.worker.id
  ]

  block_device_mappings {
    device_name = var.launch_template_block_device_mappings["device_name"]

    ebs {
      delete_on_termination = var.launch_template_block_device_mappings["delete_on_termination"]
      encrypted             = var.launch_template_block_device_mappings["encrypted"]
      iops                  = var.launch_template_block_device_mappings["iops"]
      throughput            = var.launch_template_block_device_mappings["throughput"]
      volume_size           = var.launch_template_block_device_mappings["volume_size"]
      volume_type           = var.launch_template_block_device_mappings["volume_type"]
    }
  }
}

resource "aws_autoscaling_group" "this" {
  name_prefix               = "${local.service_prefix}-worker"
  max_size                  = local.worker_max_instances
  min_size                  = local.worker_min_instances
  desired_capacity          = local.worker_desired_instances
  health_check_type         = "EC2"
  health_check_grace_period = 480
  enabled_metrics           = ["GroupTerminatingInstances", "GroupInServiceInstances", "GroupMaxSize", "GroupTotalInstances", "GroupMinSize", "GroupPendingInstances", "GroupDesiredCapacity", "GroupStandbyInstances"]
  vpc_zone_identifier       = toset(local.private_subnet_ids)
  protect_from_scale_in     = true

  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 100
    }
  }

  launch_template {
    id      = aws_launch_template.this.id
    version = "$Latest"
  }

  lifecycle {
    create_before_destroy = true
    ignore_changes        = [instance_refresh]
  }

  dynamic "tag" {
    for_each = merge(local.additional_asg_tags, data.aws_default_tags.this.tags)
    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }
}

resource "aws_autoscaling_policy" "worker_target_tracking_policy" {
  name                      = "${local.service_prefix}-worker-target-tracking-policy"
  policy_type               = "TargetTrackingScaling"
  autoscaling_group_name    = aws_autoscaling_group.this.name
  estimated_instance_warmup = 120

  target_tracking_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ASGAverageCPUUtilization"
    }

    target_value = "80"
  }
}
