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

  bfd_keystore_file_name = lookup({
    test    = "ab2d_impl_keystore"
    dev     = "ab2d_dev_keystore"
    sandbox = "ab2d_sbx_keystore"
    prod    = "ab2d_prod_keystore"
  }, local.parent_env, local.env)

  bfd_insights       = "none"
  private_subnet_ids = keys(module.platform.private_subnets)

  benv = lookup({
    "dev"     = "ab2d-dev"
    "test"    = "ab2d-east-impl"
    "prod"    = "ab2d-east-prod"
    "sandbox" = "ab2d-sbx-sandbox"
  }, local.parent_env, local.env)

  region_name = module.platform.primary_region.name
  vpc_id      = module.platform.vpc_id

  bfd_url = lookup({
    dev     = "prod-sbx.fhir.bfd.cmscloud.local"
    test    = "prod-sbx.fhir.bfd.cmscloud.local"
    prod    = "prod.bfd.fhir.cmscloud.local"
    sandbox = "prod-sbx.fhir.bfd.cmscloud.local"
  }, local.env, "prod-sbx.fhir.bfd.cmscloud.local")

  cpm_backup = "Daily Weekly Monthly" #FIXME

  bfd_keystore_location = module.platform.ssm.worker.bfd_keystore_location.value
  bfd_keystore_password = module.platform.ssm.worker.bfd_keystore_password.value
  ami_id                = data.aws_ami.ab2d_ami.id

  ec2_instance_type_worker   = module.platform.parent_env == "prod" ? "c6a.12xlarge" : "m6a.xlarge"
  ecs_task_def_cpu_worker    = module.platform.parent_env == "prod" ? 49152 : 4096
  ecs_task_def_memory_worker = module.platform.parent_env == "prod" ? 88473 : 14745
  gold_disk_name             = data.aws_ami.cms_gold.name
  image_version              = "" #FIXME aws_ami cms or ab2d?
  max_concurrent_eob_jobs    = 2
  ssh_key_name               = "burldawg"
  worker_desired_instances   = module.platform.parent_env == "prod" ? 2 : 1
  worker_min_instances       = module.platform.parent_env == "prod" ? 2 : 1
  worker_max_instances       = module.platform.parent_env == "prod" ? 4 : 2

  db_name                     = module.platform.ssm.core.database_name.value
  db_password                 = module.platform.ssm.core.database_password.value
  db_username                 = module.platform.ssm.core.database_user.value
  main_bucket                 = module.platform.ssm.core.main-bucket-name.value
  microservices_url           = module.platform.ssm.microservices.url.value
  new_relic_app_name          = module.platform.ssm.common.new_relic_app_name.value
  new_relic_license_key       = module.platform.ssm.common.new_relic_license_key.value
  slack_alert_webhooks        = module.platform.ssm.common.slack_alert_webhooks.value
  slack_trace_webhooks        = module.platform.ssm.common.slack_trace_webhooks.value
  aws_account_cms_gold_images = module.platform.ssm.accounts.cms-gold-images.value

  additional_asg_tags = {
    Name           = "${local.service_prefix}-worker"
    stack          = local.env
    purpose        = "ECS container instance"
    sensitivity    = "Public"
    "cpm backup"   = local.cpm_backup #FIXME
    purchase_type  = "On-Demand"      #FIXME
    os_license     = "Amazon Linux 2023"
    gold_disk_name = local.gold_disk_name
    image_version  = local.image_version
  }
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
  from_port                = "5432"
  to_port                  = "5432"
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = data.aws_security_group.db.id
}

resource "aws_security_group_rule" "efs_ingress" {
  type                     = "ingress"
  description              = "NFS"
  from_port                = "2049"
  to_port                  = "2049"
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.worker.id
  security_group_id        = data.aws_security_group.efs.id
}


resource "aws_ecs_cluster" "ab2d_worker" {
  name = "${local.service_prefix}-worker"
}

data "aws_sqs_queue" "events" {
  name = "ab2d-${local.env}-events-sqs"
}

resource "aws_ecs_task_definition" "worker" {
  #ts:skip=AWS.EcsCluster.NetworkSecurity.High.0104 assigned in the launch configuration for EC2 instances
  family = "${local.service_prefix}-worker"
  volume {
    name      = "efs"
    host_path = "/mnt/efs"
  }
  container_definitions = templatefile("${path.module}/templates/worker_definition.tpl",
    {
      bfd_keystore_location           = local.bfd_keystore_location
      bfd_keystore_password           = local.bfd_keystore_password
      bfd_url                         = local.bfd_url
      bfd_insights                    = lower(local.bfd_insights) #FIXME?
      db_host                         = data.aws_db_instance.this.address
      db_name                         = local.db_name
      db_password                     = local.db_password
      db_port                         = "5432"
      db_username                     = local.db_username
      ecs_task_def_cpu_worker         = local.ecs_task_def_cpu_worker
      ecs_task_def_memory_worker      = local.ecs_task_def_memory_worker
      execution_env                   = local.benv
      image_version                   = data.aws_ecr_image.worker.image_tags[0]
      max_concurrent_eob_jobs         = local.max_concurrent_eob_jobs
      new_relic_app_name              = local.new_relic_app_name
      new_relic_license_key           = local.new_relic_license_key
      slack_alert_webhooks            = local.slack_alert_webhooks
      slack_trace_webhooks            = local.slack_trace_webhooks
      sqs_url                         = data.aws_sqs_queue.events.url
      sqs_feature_flag                = true
      properties_service_url          = local.microservices_url
      properties_service_feature_flag = true
      contracts_service_feature_flag  = true
      worker_image                    = data.aws_ecr_image.worker.image_uri
    }
  )
  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"
  execution_role_arn       = data.aws_iam_role.worker.arn
}

resource "aws_ecs_service" "worker" {
  name                               = "${local.service_prefix}-worker"
  cluster                            = aws_ecs_cluster.ab2d_worker.id
  task_definition                    = coalesce(var.override_task_definition_arn, aws_ecs_task_definition.worker.arn)
  launch_type                        = "EC2"
  scheduling_strategy                = "DAEMON"
  force_new_deployment               = anytrue([var.force_worker_deployment, var.worker_service_image_tag != null])
  deployment_minimum_healthy_percent = 100
}

resource "aws_launch_template" "ab2d_worker" {
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
        env                    = lower(local.env),
        cluster_name           = "${local.service_prefix}-worker"
        efs_id                 = data.aws_efs_file_system.this.file_system_id,
        bfd_keystore_file_name = local.bfd_keystore_file_name
        aws_region             = local.region_name
        bucket_name            = local.main_bucket
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

resource "aws_autoscaling_group" "asg" {
  name_prefix               = "${local.service_prefix}-worker"
  max_size                  = local.worker_max_instances
  min_size                  = local.worker_min_instances
  desired_capacity          = local.worker_desired_instances
  health_check_type         = "EC2"
  health_check_grace_period = 480
  enabled_metrics           = ["GroupTerminatingInstances", "GroupInServiceInstances", "GroupMaxSize", "GroupTotalInstances", "GroupMinSize", "GroupPendingInstances", "GroupDesiredCapacity", "GroupStandbyInstances"]
  vpc_zone_identifier       = toset(local.private_subnet_ids)

  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 100
    }
  }

  launch_template {
    id      = aws_launch_template.ab2d_worker.id
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
  autoscaling_group_name    = aws_autoscaling_group.asg.name
  estimated_instance_warmup = 120

  target_tracking_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ASGAverageCPUUtilization"
    }

    target_value = "80"
  }
}
