resource "aws_security_group" "api" {
  name        = "${lower(var.env)}-api-sg"
  description = "API security group"
  vpc_id      = var.vpc_id
  tags = {
    Name = "${lower(var.env)}-api-sg"
  }
}

resource "aws_security_group_rule" "node_access" {
  type        = "ingress"
  description = "Node Access"
  from_port   = "-1"
  to_port     = "-1"
  protocol    = "-1"
  source_security_group_id = aws_security_group.api.id
  security_group_id = var.controller_sec_group_id
}

resource "aws_security_group_rule" "host_port" {
  type        = "ingress"
  description = "Host Port"
  from_port   = var.host_port
  to_port     = var.host_port
  protocol    = "tcp"
  source_security_group_id = aws_security_group.api.id
  security_group_id = aws_security_group.api.id
}

resource "aws_security_group_rule" "controller_access" {
  type        = "ingress"
  description = "Controller Access"
  from_port   = "-1"
  to_port     = "-1"
  protocol    = "-1"
  source_security_group_id = var.controller_sec_group_id
  security_group_id = aws_security_group.api.id
}

# LSH SKIP FOR NOW BEGIN
# resource "aws_security_group_rule" "vpn_http" {
#   type        = "ingress"
#   description = "VPN Access"
#   from_port   = var.host_port
#   to_port     = var.host_port
#   protocol    = "tcp"
#   cidr_blocks = ["10.252.0.0/16", "10.232.32.0/19", "10.251.0.0/16", "52.20.26.200/32", "34.196.35.156/32"]
#   security_group_id = aws_security_group.api.id
# }
# LSH SKIP FOR NOW END

# LSH SKIP FOR NOW BEGIN
# resource "aws_security_group_rule" "vpn_https" {
#   type        = "ingress"
#   description = "VPN Access"
#   from_port   = "443"
#   to_port     = "443"
#   protocol    = "tcp"
#   cidr_blocks = ["10.252.0.0/16", "10.232.32.0/19", "10.251.0.0/16", "52.20.26.200/32", "34.196.35.156/32"]
#   security_group_id = aws_security_group.api.id
# }
# LSH SKIP FOR NOW END

# LSH SKIP FOR NOW BEGIN
# resource "aws_security_group_rule" "kong_http" {
#   type        = "ingress"
#   description = "Kong"
#   from_port   = var.host_port
#   to_port     = var.host_port
#   protocol    = "tcp"
#   cidr_blocks = ["34.204.33.165/32","34.226.82.144/32","34.200.65.22/32","34.227.6.19/32"]
#   security_group_id = aws_security_group.api.id
# }
# LSH SKIP FOR NOW END

# LSH SKIP FOR NOW BEGIN
# resource "aws_security_group_rule" "kong_https" {
#   type        = "ingress"
#   description = "Kong"
#   from_port   = "443"
#   to_port     = "443"
#   protocol    = "tcp"
#   cidr_blocks = ["34.204.33.165/32","34.226.82.144/32","34.200.65.22/32","34.227.6.19/32"]
#   security_group_id = aws_security_group.api.id
# }
# LSH SKIP FOR NOW END

resource "aws_security_group_rule" "egress_api" {
  type        = "egress"
  description = "Allow all egress"
  from_port   = "0"
  to_port     = "0"
  protocol    = "-1"
  cidr_blocks = ["0.0.0.0/0"]
  security_group_id = aws_security_group.api.id
}

resource "aws_security_group_rule" "db_access_api" {
  type        = "ingress"
  description = "${lower(var.env)} api connections"
  from_port   = "5432"
  to_port     = "5432"
  protocol    = "tcp"
  source_security_group_id = aws_security_group.api.id
  security_group_id = var.db_sec_group_id
}

resource "aws_security_group" "load_balancer" {
  name        = "${lower(var.env)}-load-balancer-sg"
  description = "API security group"
  vpc_id      = var.vpc_id
  tags = {
    Name = "${lower(var.env)}-load-balancer-sg"
  }
}

resource "aws_security_group_rule" "load_balancer_access" {
  type        = "ingress"
  description = "${lower(var.env)} website access"
  from_port   = "80"
  to_port     = "80"
  protocol    = "tcp"
  cidr_blocks = ["0.0.0.0/0"]
  security_group_id = aws_security_group.load_balancer.id
}

resource "aws_security_group_rule" "efs_ingress" {
  type        = "ingress"
  description = "NFS"
  from_port   = "2049"
  to_port     = "2049"
  protocol    = "tcp"
  source_security_group_id = aws_security_group.api.id
  security_group_id = var.efs_security_group_id
}

resource "aws_efs_mount_target" "alpha" {
  file_system_id  = var.efs_id
  subnet_id       = var.alpha
  security_groups = [var.efs_security_group_id]
}

resource "aws_efs_mount_target" "beta" {
  file_system_id  = var.efs_id
  subnet_id      =  var.beta
  security_groups = [var.efs_security_group_id]
}

resource "aws_ecs_cluster" "ab2d_api" {
  name = "${lower(var.env)}-api"
}

resource "aws_ecs_task_definition" "api" {
  family = "api"
  volume {
    name      = "efs"
    host_path = "/mnt/efs"
  }
  container_definitions = <<JSON
  [
    {
      "name": "ab2d-api",
      "image": "${var.ecr_repo_aws_account}.dkr.ecr.us-east-1.amazonaws.com/ab2d_api:${var.image_version}",
      "essential": true,
      "memory": 2048,
      "portMappings": [
        {
          "containerPort": ${var.container_port},
          "hostPort": ${var.ecs_task_definition_host_port}
        }
      ],
      "mountPoints": [
        {
	  "containerPath": "/mnt/efs",
	  "sourceVolume": "efs"
	}
      ],
      "environment" : [
        {
	  "name" : "AB2D_DB_HOST",
	  "value" : "${var.db_host}"
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
	  "name" : "AB2D_DB_PASSWORD",
	  "value" : "${var.db_password}"
	},
	{
	  "name" : "AB2D_DB_DATABASE",
	  "value" : "${var.db_name}"
	},
        {
	  "name" : "AB2D_EFS_MOUNT",
	  "value" : "/mnt/efs"
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
  cpu = 1024
  memory = 2048
  execution_role_arn = "arn:aws:iam::${var.aws_account_number}:role/Ab2dInstanceRole"
}

resource "aws_lb" "api" {
  name = "${lower(var.env)}"
  internal = false
  load_balancer_type = "application"
  security_groups = [aws_security_group.api.id, aws_security_group.load_balancer.id]
  subnets = var.controller_subnet_ids
  enable_deletion_protection = true
  enable_cross_zone_load_balancing = true

  access_logs {
    bucket = var.logging_bucket
    prefix = "${lower(var.env)}"
    enabled = true
  }
}

resource "aws_lb_target_group" "api" {
  name = "${lower(var.env)}-api-tg"
  port = var.host_port
  protocol = "HTTP"
  vpc_id = var.vpc_id

  health_check {
    healthy_threshold = 5
    unhealthy_threshold = 2
    timeout = 2
    path = "/swagger-ui.html"
    interval = 5
  }
}

resource "aws_lb_listener" "api" {
  load_balancer_arn = aws_lb.api.arn
  port = var.host_port
  protocol = "HTTP"

  default_action {
    target_group_arn = aws_lb_target_group.api.arn
    type = "forward"
  }
}

resource "aws_ecs_service" "api" {
  depends_on = ["aws_lb.api"]
  name = "${lower(var.env)}-api"
  cluster = aws_ecs_cluster.ab2d_api.id
  task_definition = var.override_task_definition_arn != "" ? var.override_task_definition_arn : aws_ecs_task_definition.api.arn
  desired_count = 5
  launch_type = "EC2"
  scheduling_strategy = "DAEMON"

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name = "ab2d-api"
    container_port = var.container_port
  }
}

# LSH SKIP FOR NOW BEGIN
# security_groups = [aws_security_group.api.id,var.enterprise-tools-sec-group-id,var.vpn-private-sec-group-id]
# LSH SKIP FOR NOW BEGIN
resource "aws_launch_configuration" "launch_config" {
  name_prefix = "${lower(var.env)}-api-"
  image_id = var.ami_id
  instance_type = var.instance_type
  iam_instance_profile = var.iam_instance_profile
  key_name = var.ssh_key_name
  security_groups = [aws_security_group.api.id]  
  user_data = templatefile("${path.module}/userdata.tpl",{ env = "${lower(var.env)}", cluster_name = "${lower(var.env)}-api", efs_id = var.efs_id })
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
  target_group_arns = [aws_lb_target_group.api.arn]
  enabled_metrics = ["GroupTerminatingInstances", "GroupInServiceInstances", "GroupMaxSize", "GroupTotalInstances", "GroupMinSize", "GroupPendingInstances", "GroupDesiredCapacity", "GroupStandbyInstances"]
  wait_for_elb_capacity = var.autoscale_group_wait
  vpc_zone_identifier = var.node_subnet_ids
  lifecycle { create_before_destroy = true }

  tags = [
    {
      key = "Name"
      value = "${lower(var.env)}-api"
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
