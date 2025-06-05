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
  root_module  = "https://github.com/CMSgov/ab2d/tree/main/ops/services/30-api"
  service      = local.service
  ssm_root_map = local.ssm_root_map
}

locals {
  default_tags = module.platform.default_tags
  env          = terraform.workspace
  service      = "api"

  ssm_root_map = {
    common        = "/ab2d/${local.parent_env}/common"
    core          = "/ab2d/${local.parent_env}/core"
    microservices = "/ab2d/${local.parent_env}/microservices"
    contracts     = "/ab2d/mgmt/pdps/nonsensitive/contracts-csv"
    cidrs         = "/ab2d/mgmt/pdps/sensitive/cidr-blocks-csv"
    accounts      = "/ab2d/mgmt/aws-account-numbers"
    mgmt_ipv4     = "/cdap/mgmt/public_nat_ipv4"
  }

  #TODO in honor of Ben "Been Jammin'" Hesford
  benv = lookup({
    dev     = "ab2d-dev"
    test    = "ab2d-east-impl"
    prod    = "ab2d-east-prod"
    sandbox = "ab2d-sbx-sandbox"
  }, local.parent_env, local.env)

  aws_region                            = module.platform.primary_region.name
  ab2d_keystore_location                = "classpath:ab2d.p12"
  ab2d_keystore_password                = module.platform.ssm.core.keystore_password.value
  ab2d_okta_jwt_issuer                  = module.platform.ssm.core.okta_jwt_issuer.value
  ab2d_v2_enabled                       = true
  alb_internal                          = false
  alb_listener_certificate_arn          = module.platform.is_ephemeral_env ? null : data.aws_acm_certificate.issued[0].arn
  alb_listener_port                     = module.platform.is_ephemeral_env ? 80 : 443
  alb_listener_protocol                 = module.platform.is_ephemeral_env ? "HTTP" : "HTTPS"
  ami_id                                = data.aws_ami.ab2d.id
  api_desired_instances                 = module.platform.parent_env == "prod" ? 2 : 1
  api_max_instances                     = module.platform.parent_env == "prod" ? 4 : 2
  api_min_instances                     = module.platform.parent_env == "prod" ? 2 : 1
  bfd_insights                          = "none" #FIXME?
  container_port                        = 8443
  cpm_backup                            = "Daily Weekly Monthly" #FIXME
  db_name                               = module.platform.ssm.core.database_name.value
  db_password                           = module.platform.ssm.core.database_password.value
  db_port                               = 5432
  db_username                           = module.platform.ssm.core.database_user.value
  ec2_instance_type_api                 = "m6a.xlarge"
  ecs_task_def_cpu_api                  = 4096
  ecs_task_def_memory_api               = 14745
  gold_disk_name                        = data.aws_ami.cms.name
  hpms_api_params                       = module.platform.ssm.core.hpms_api_params.value
  hpms_auth_key_id                      = module.platform.ssm.core.hpms_auth_key_id.value
  hpms_auth_key_secret                  = module.platform.ssm.core.hpms_auth_key_secret.value
  hpms_url                              = module.platform.ssm.core.hpms_url.value
  image_version                         = "" #FIXME aws_ami cms or ab2d?
  kms_master_key_id                     = nonsensitive(module.platform.kms_alias_primary.target_key_arn)
  launch_template_block_device_mappings = var.launch_template_block_device_mappings
  main_bucket                           = module.platform.ssm.core.main-bucket-name.value
  microservices_url                     = module.platform.ssm.microservices.url.value
  network_access_logs_bucket            = module.platform.ssm.core.network-access-logs-bucket-name.value
  new_relic_app_name                    = module.platform.ssm.common.new_relic_app_name.value
  new_relic_license_key                 = module.platform.ssm.common.new_relic_license_key.value
  private_subnet_ids                    = keys(module.platform.private_subnets)
  public_subnet_ids                     = keys(module.platform.public_subnets)
  slack_alert_webhooks                  = module.platform.ssm.common.slack_alert_webhooks.value
  slack_trace_webhooks                  = module.platform.ssm.common.slack_trace_webhooks.value
  ssh_key_name                          = "burldawg" #FIXME
  vpc_id                                = module.platform.vpc_id
  cloudwatch_sns_topic                  = data.aws_sns_topic.cloudwatch_alarms.arn
  aws_account_cms_gold_images           = module.platform.ssm.accounts.cms-gold-images.value

  pdp_map = { for k in keys(module.platform.ssm.cidrs) : k => { "cidrs" = nonsensitive(module.platform.ssm.cidrs[k].value), "contracts" = nonsensitive(module.platform.ssm.contracts[k].value) } }

  additional_asg_tags = {
    Name           = "${local.service_prefix}-api"
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

data "aws_default_tags" "this" {}

resource "aws_security_group" "pdp" {
  name        = "${local.service_prefix}-pdp"
  description = "PDP security group"
  vpc_id      = local.vpc_id
  tags = {
    Name = "${local.service_prefix}-pdp"
  }
}

resource "aws_security_group_rule" "node_access" {
  type                     = "ingress"
  description              = "Node Access"
  from_port                = "-1"
  to_port                  = "-1"
  protocol                 = "-1"
  source_security_group_id = data.aws_security_group.api.id
  security_group_id        = module.platform.security_groups["remote-management"].id
}

resource "aws_security_group_rule" "host_port" {
  type                     = "ingress"
  description              = "Host Port"
  from_port                = local.alb_listener_port
  to_port                  = local.alb_listener_port
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.api.id
  security_group_id        = data.aws_security_group.api.id
}

resource "aws_security_group" "load_balancer" {
  name        = "${local.service_prefix}-load-balancer-sg"
  description = "API load balancer security group"
  vpc_id      = local.vpc_id
  tags = {
    Name = "${local.service_prefix}-load-balancer-sg"
  }
}

resource "aws_security_group_rule" "load_balancer_access_mgmt" {
  for_each = nonsensitive(module.platform.ssm.mgmt_ipv4)

  type              = "ingress"
  description       = "Access from ${each.key}"
  from_port         = local.alb_listener_port
  to_port           = local.alb_listener_port
  protocol          = "tcp"
  cidr_blocks       = ["${each.value.value}/32"]
  security_group_id = aws_security_group.load_balancer.id
}

resource "aws_security_group_rule" "load_balancer_access_nat" {
  for_each = nonsensitive(module.platform.nat_gateways)

  type              = "ingress"
  description       = "${local.env} ${lookup(each.value.tags, "Name", "nat")} website access"
  from_port         = local.alb_listener_port
  to_port           = local.alb_listener_port
  protocol          = "tcp"
  cidr_blocks       = ["${each.value.public_ip}/32"]
  security_group_id = aws_security_group.load_balancer.id
}

resource "aws_security_group_rule" "pdp" {
  for_each = nonsensitive(local.pdp_map)

  type              = "ingress"
  description       = "${replace(each.key, "-", " ")}: ${each.value["contracts"]}"
  from_port         = local.alb_listener_port
  to_port           = local.alb_listener_port
  protocol          = "tcp"
  cidr_blocks       = split(", ", each.value["cidrs"])
  security_group_id = aws_security_group.pdp.id
}

resource "aws_security_group_rule" "open_access_sandbox" {
  count             = local.env == "ab2d-sbx-sandbox" ? 1 : 0
  type              = "ingress"
  description       = "Rule to open SBX to public"
  from_port         = local.alb_listener_port
  to_port           = local.alb_listener_port
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.pdp.id
}

resource "aws_security_group_rule" "cdap_ingress" {
  type              = "ingress"
  description       = "CDAP Platform Ingress"
  from_port         = "443"
  to_port           = "443"
  protocol          = "tcp"
  cidr_blocks       = [module.platform.platform_cidr]
  security_group_id = aws_security_group.load_balancer.id
}

resource "aws_security_group_rule" "efs_ingress" {
  type                     = "ingress"
  description              = "NFS"
  from_port                = "2049"
  to_port                  = "2049"
  protocol                 = "tcp"
  source_security_group_id = data.aws_security_group.api.id
  security_group_id        = data.aws_security_group.efs.id
}


resource "aws_ecs_cluster" "ab2d_api" {
  name = "${local.service_prefix}-api"

  setting {
    name  = "containerInsights"
    value = module.platform.is_ephemeral_env ? "disabled" : "enabled"
  }
}

resource "aws_ecs_task_definition" "api" {
  #ts:skip=AWS.EcsCluster.NetworkSecurity.High.0104 vpc is assigned via a security group on the aws_lb
  family                   = "${local.service_prefix}-api"
  network_mode             = "bridge"
  execution_role_arn       = data.aws_iam_role.api.arn
  requires_compatibilities = ["EC2"]

  volume {
    configure_at_launch = false
    name                = "efs"
    host_path           = "/mnt/efs"
  }

  container_definitions = templatefile("${path.module}/templates/api_definition.tpl", {
    ab2d_keystore_location          = local.ab2d_keystore_location
    ab2d_keystore_password          = local.ab2d_keystore_password
    ab2d_okta_jwt_issuer            = local.ab2d_okta_jwt_issuer
    ab2d_v2_enabled                 = local.ab2d_v2_enabled
    alb_listener_port               = local.alb_listener_port
    bfd_insights                    = lower(local.bfd_insights)
    container_port                  = local.container_port
    db_host                         = data.aws_db_instance.this.address
    db_name                         = local.db_name
    db_password                     = local.db_password
    db_port                         = local.db_port
    db_username                     = local.db_username
    api_image                       = data.aws_ecr_image.api.image_uri
    ecs_task_def_cpu_api            = local.ecs_task_def_cpu_api
    ecs_task_def_memory_api         = local.ecs_task_def_memory_api
    env                             = lower(local.env)
    execution_env                   = local.benv
    hpms_api_params                 = local.hpms_api_params
    hpms_auth_key_id                = local.hpms_auth_key_id
    hpms_auth_key_secret            = local.hpms_auth_key_secret
    hpms_url                        = local.hpms_url
    new_relic_app_name              = local.new_relic_app_name
    new_relic_license_key           = local.new_relic_license_key
    slack_alert_webhooks            = local.slack_alert_webhooks
    slack_trace_webhooks            = local.slack_trace_webhooks
    sqs_url                         = data.aws_sqs_queue.events.url
    sqs_feature_flag                = true
    properties_service_url          = local.microservices_url
    properties_service_feature_flag = true
    contracts_service_feature_flag  = true
  })
}

resource "aws_ecs_service" "api" {
  name                               = "${local.service_prefix}-api"
  cluster                            = aws_ecs_cluster.ab2d_api.id
  task_definition                    = coalesce(var.override_task_definition_arn, aws_ecs_task_definition.api.arn)
  launch_type                        = "EC2"
  scheduling_strategy                = "DAEMON"
  force_new_deployment               = anytrue([var.force_api_deployment, var.api_service_image_tag != null])
  deployment_minimum_healthy_percent = 100
  health_check_grace_period_seconds  = 600
  load_balancer {
    target_group_arn = aws_lb_target_group.ab2d_api.arn
    container_name   = "ab2d-api"
    container_port   = local.container_port
  }
}

resource "aws_launch_template" "ab2d_api" {
  name          = "${local.service_prefix}-api"
  image_id      = local.ami_id
  instance_type = local.ec2_instance_type_api
  key_name      = local.ssh_key_name

  iam_instance_profile {
    name = aws_iam_instance_profile.api_profile.name
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
        env          = lower(local.env),
        cluster_name = "${local.service_prefix}-api",
        efs_id       = data.aws_efs_file_system.this.file_system_id
        aws_region   = local.aws_region
        bucket_name  = local.main_bucket
      }
    )
  )

  vpc_security_group_ids = [
    data.aws_security_group.api.id
  ]

  block_device_mappings {
    device_name = local.launch_template_block_device_mappings["device_name"]

    ebs {
      volume_type           = local.launch_template_block_device_mappings["volume_type"]
      volume_size           = local.launch_template_block_device_mappings["volume_size"]
      iops                  = local.launch_template_block_device_mappings["iops"]
      throughput            = local.launch_template_block_device_mappings["throughput"]
      delete_on_termination = local.launch_template_block_device_mappings["delete_on_termination"]
      encrypted             = local.launch_template_block_device_mappings["encrypted"]
    }
  }
}

locals {
}

resource "aws_autoscaling_group" "asg" {
  name_prefix               = "${local.service_prefix}-api"
  max_size                  = local.api_max_instances
  min_size                  = local.api_min_instances
  desired_capacity          = local.api_desired_instances
  health_check_type         = "ELB"
  health_check_grace_period = 600
  default_cooldown          = 300
  target_group_arns         = [aws_lb_target_group.ab2d_api.arn]
  enabled_metrics           = ["GroupTerminatingInstances", "GroupInServiceInstances", "GroupMaxSize", "GroupTotalInstances", "GroupMinSize", "GroupPendingInstances", "GroupDesiredCapacity", "GroupStandbyInstances"]
  wait_for_elb_capacity     = local.api_desired_instances
  vpc_zone_identifier       = toset(local.private_subnet_ids)

  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 100
    }
  }

  launch_template {
    id      = aws_launch_template.ab2d_api.id
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
resource "aws_autoscaling_policy" "api_target_tracking_policy" {
  name                      = "${local.service_prefix}-api-target-tracking-policy"
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

resource "aws_sns_topic" "api" {
  name              = "${local.service_prefix}-api-healthy-host"
  kms_master_key_id = local.kms_master_key_id
}

resource "aws_cloudwatch_metric_alarm" "health" {
  alarm_name          = "${local.service_prefix}-api-healthy-host"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "20"
  metric_name         = "HealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Maximum"
  threshold           = "1"
  alarm_description   = "Healthy host count for API target group"
  alarm_actions       = [aws_sns_topic.api.arn]
  ok_actions          = [aws_sns_topic.api.arn]

  dimensions = {
    LoadBalancer = aws_lb.ab2d_api.arn_suffix
    TargetGroup  = aws_lb_target_group.ab2d_api.arn_suffix
  }
}

resource "aws_lb" "ab2d_api" {
  #TODO Consider using name_prefix for ephemeral environments... thhey may only be up to 6-characters
  name               = "${local.service_prefix}-api"
  depends_on         = [aws_lb_target_group.ab2d_api]
  internal           = local.alb_internal
  load_balancer_type = "application"

  security_groups = [
    data.aws_security_group.api.id,
    aws_security_group.load_balancer.id,
    aws_security_group.pdp.id,
    module.platform.security_groups["zscaler-public"].id,
  ]

  subnets = local.public_subnet_ids

  enable_deletion_protection       = contains(["prod", "sandbox", "test", "dev"], local.env)
  enable_cross_zone_load_balancing = true
  drop_invalid_header_fields       = true

  access_logs {
    bucket  = local.network_access_logs_bucket
    prefix  = "${local.service_prefix}-${local.service}"
    enabled = true
  }
}

resource "aws_lb_target_group" "ab2d_api" {
  name     = "${local.service_prefix}-api"
  port     = local.alb_listener_port
  protocol = "HTTPS"
  vpc_id   = local.vpc_id

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 5
    timeout             = 10
    protocol            = "HTTPS"
    path                = "/health"
    interval            = 30
  }
}

resource "aws_lb_listener" "ab2d_api" {
  load_balancer_arn = aws_lb.ab2d_api.arn
  port              = local.alb_listener_port
  protocol          = local.alb_listener_protocol
  certificate_arn   = local.alb_listener_certificate_arn

  default_action {
    target_group_arn = aws_lb_target_group.ab2d_api.arn
    type             = "forward"
  }
}
