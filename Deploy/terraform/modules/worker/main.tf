resource "aws_security_group" "worker" {
  name        = "${lower(var.env)}-worker-sg"
  description = "Worker security group"
  vpc_id      = var.vpc_id
  tags = {
    Name = "${lower(var.env)}-worker-sg"
  }
}

resource "aws_security_group_rule" "controller_access" {
  type        = "ingress"
  description = "Controller Access"
  from_port   = "-1"
  to_port     = "-1"
  protocol    = "-1"
  source_security_group_id = var.controller_sec_group_id
  security_group_id = aws_security_group.worker.id
}

resource "aws_security_group_rule" "egress_worker" {
  type        = "egress"
  description = "Allow all egress"
  from_port   = "0"
  to_port     = "0"
  protocol    = "-1"
  cidr_blocks = ["0.0.0.0/0"]
  security_group_id = aws_security_group.worker.id
}

resource "aws_security_group_rule" "db_access_worker" {
  type        = "ingress"
  description = "${lower(var.env)} worker connections"
  from_port   = "5432"
  to_port     = "5432"
  protocol    = "tcp"
  source_security_group_id = aws_security_group.worker.id
  security_group_id = var.db_sec_group_id
}

resource "aws_security_group_rule" "efs_ingress" {
  type        = "ingress"
  description = "NFS"
  from_port   = "2049"
  to_port     = "2049"
  protocol    = "tcp"
  source_security_group_id = aws_security_group.worker.id
  security_group_id = var.efs_security_group_id
}

resource "aws_ecs_cluster" "ab2d_worker" {
  name = "${lower(var.env)}-worker"
}

resource "aws_ecs_task_definition" "worker" {
  family = "worker"
  volume {
    name      = "efs"
    host_path = "/mnt/efs"
  }
  container_definitions = <<JSON
  [
    {
      "name": "${lower(var.env)}-worker",
      "image": "${var.ecr_repo_aws_account}.dkr.ecr.us-east-1.amazonaws.com/ab2d_worker:${lower(var.env)}-latest",
      "essential": true,
      "memory": ${var.ecs_task_def_memory},
      "mountPoints": [
        {
	  "containerPath": "/mnt/efs",
	  "sourceVolume": "efs"
	}
      ],
      "environment": [
	{
	  "name" : "AB2D_BFD_KEYSTORE_LOCATION",
	  "value" : "${var.bfd_keystore_location}"
	},
	{
	  "name" : "AB2D_BFD_KEYSTORE_PASSWORD",
	  "value" : "${var.bfd_keystore_password}"
	},
	{
	  "name" : "AB2D_BFD_URL",
	  "value" : "${var.bfd_url}"
	},
	{
	  "name" : "AB2D_DB_DATABASE",
	  "value" : "${var.db_name}"
	},
        {
	  "name" : "AB2D_DB_HOST",
	  "value" : "${var.db_host}"
	},
	{
	  "name" : "AB2D_DB_PASSWORD",
	  "value" : "${var.db_password}"
	},
	{
	  "name" : "AB2D_DB_PORT",
	  "value" : "${var.db_port}"
	},
	{
	  "name" : "AB2D_DB_USER",
	  "value" : "${var.db_username}"
	},
	{
	  "name" : "AB2D_EFS_MOUNT",
	  "value" : "/mnt/efs"
	},
	{
	  "name" : "AB2D_HICN_HASH_PEPPER",
	  "value" : "${var.hicn_hash_pepper}"
	},
	{
	  "name" : "AB2D_HICN_HASH_ITER",
	  "value" : "${var.hicn_hash_iter}"
	},
        {
	  "name" : "NEW_RELIC_APP_NAME",
	  "value" : "${var.new_relic_app_name}"
	},
        {
	  "name" : "NEW_RELIC_LICENSE_KEY",
	  "value" : "${var.new_relic_license_key}"
	}
      ],
      "logConfiguration": {
        "logDriver": "syslog"
      },
      "healthCheck": null
    }
  ]
JSON
  requires_compatibilities = ["EC2"]
  network_mode = "bridge"
  cpu = var.ecs_task_def_cpu
  memory = var.ecs_task_def_memory
  execution_role_arn = "arn:aws:iam::${var.aws_account_number}:role/Ab2dInstanceRole"
}

resource "aws_ecs_service" "worker" {
  name = "${lower(var.env)}-worker"
  cluster = aws_ecs_cluster.ab2d_worker.id
  task_definition = var.override_task_definition_arn != "" ? var.override_task_definition_arn : aws_ecs_task_definition.worker.arn
  desired_count = 5
  launch_type = "EC2"
  scheduling_strategy = "DAEMON"
}

# LSH SKIP FOR NOW BEGIN
# security_groups = [aws_security_group.worker.id,var.enterprise-tools-sec-group-id,var.vpn-private-sec-group-id]
# LSH SKIP FOR NOW END
resource "aws_launch_configuration" "launch_config" {
  name_prefix = "${lower(var.env)}-worker-"
  image_id = var.ami_id
  instance_type = var.instance_type
  iam_instance_profile = var.iam_instance_profile
  key_name = var.ssh_key_name
  security_groups = [aws_security_group.worker.id]
  user_data = templatefile("${path.module}/userdata.tpl",{ env = "${lower(var.env)}", cluster_name = "${lower(var.env)}-worker", efs_id = var.efs_id, bfd_keystore_file_name = var.bfd_keystore_file_name })
  lifecycle { create_before_destroy = true }
}

resource "aws_autoscaling_group" "asg" {
  depends_on = ["aws_launch_configuration.launch_config"]
  availability_zones = ["us-east-1a","us-east-1b","us-east-1c","us-east-1d","us-east-1e"]
  name = aws_launch_configuration.launch_config.name
  max_size = var.max_instances
  min_size = var.min_instances
  desired_capacity = var.desired_instances
  health_check_type = "EC2"
  launch_configuration = aws_launch_configuration.launch_config.name
  enabled_metrics = ["GroupTerminatingInstances", "GroupInServiceInstances", "GroupMaxSize", "GroupTotalInstances", "GroupMinSize", "GroupPendingInstances", "GroupDesiredCapacity", "GroupStandbyInstances"]
  vpc_zone_identifier = var.node_subnet_ids
  lifecycle { create_before_destroy = true }

  tags = [
    {
      key = "Name"
      value = "${lower(var.env)}-worker"
      propagate_at_launch = true
    },
    {
      key = "application"
      value = "ab2d"
      propagate_at_launch = true
    },
    {
      key = "stack"
      value = "${lower(var.env)}"
      propagate_at_launch = true
    },
    {
      key = "purpose"
      value = "ECS container instance"
      propagate_at_launch = true
    },
    {
      key = "sensitivity"
      value = "Public"
      propagate_at_launch = true
    },
    {
      key = "maintainer"
      value = "lonnie.hanekamp@semanticbits.com"
      propagate_at_launch = true
    },
    {
      key = "cpm backup"
      value = "NoBackup"
      propagate_at_launch = true
    },
    {
      key = "purchase_type"
      value = "On-Demand"
      propagate_at_launch = true
    },
    {
      key = "os_license"
      value = "Red Hat Enterprise Linux"
      propagate_at_launch = true
    },
    {
      key = "gold_disk_name"
      value = var.gold_disk_name
      propagate_at_launch = true
    },
    {
      key = "business"
      value = "CMS"
      propagate_at_launch = true
    }
  ]
}

resource "aws_autoscaling_policy" "percent_capacity" {
  name = "percent_capacity"
  scaling_adjustment = var.percent_capacity_increase
  adjustment_type = "PercentChangeInCapacity"
  cooldown = 300
  autoscaling_group_name = aws_autoscaling_group.asg.name
}

resource "aws_autoscaling_policy" "target_cpu" {
  name = "target_CPU"
  autoscaling_group_name = aws_autoscaling_group.asg.name
  policy_type = "TargetTrackingScaling"

  target_tracking_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ASGAverageCPUUtilization"
    }
    target_value = 80.0
  }
}
