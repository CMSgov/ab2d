resource "aws_security_group" "deployment_controller" {
  name        = "ab2d-deployment-controller-${var.env}"
  description = "Deployment Controller"
  vpc_id      = var.vpc_id
}

resource "aws_security_group_rule" "node_access" {
  type        = "ingress"
  description = "Node Access"
  from_port   = "-1"
  to_port     = "-1"
  protocol    = "-1"
  source_security_group_id = aws_security_group.api.id
  security_group_id = aws_security_group.deployment_controller.id
}

# *** TO DO ***: eliminate this after VPN access is setup
resource "aws_security_group_rule" "whitelist_lonnie" {
  type        = "ingress"
  description = "Whitelist Lonnie"
  from_port   = "22"
  to_port     = "22"
  protocol    = "TCP"
  cidr_blocks = ["152.208.13.223/32"]
  security_group_id = aws_security_group.deployment_controller.id
}

resource "aws_security_group_rule" "egress_controller" {
  type        = "egress"
  description = "Allow all egress"
  from_port   = "0"
  to_port     = "0"
  protocol    = "-1"
  cidr_blocks = ["0.0.0.0/0"]
  security_group_id = aws_security_group.deployment_controller.id
}

resource "aws_security_group" "api" {
  name        = "ab2d-api-${var.env}"
  description = "API security group"
  vpc_id      = var.vpc_id
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
  source_security_group_id = aws_security_group.deployment_controller.id
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
  description = "App connections"
  from_port   = "5432"
  to_port     = "5432"
  protocol    = "tcp"
  source_security_group_id = aws_security_group.api.id
  security_group_id = var.db_sec_group_id
}

resource "aws_security_group_rule" "db_access_from_controller" {
  type        = "ingress"
  description = "Deployment Controller"
  from_port   = "5432"
  to_port     = "5432"
  protocol    = "tcp"
  source_security_group_id = aws_security_group.deployment_controller.id
  security_group_id = var.db_sec_group_id
}

resource "random_shuffle" "public_subnets" {
  input = var.controller_subnet_ids
  result_count = 1
}

# LSH SKIP FOR NOW BEGIN
# vpc_security_group_ids = [aws_security_group.deployment_controller.id,var.enterprise-tools-sec-group-id,var.vpn-private-sec-group-id]
# LSH SKIP FOR NOW END
resource "aws_instance" "deployment_controller" {
  ami = var.ami_id
  instance_type = var.instance_type
  vpc_security_group_ids = [aws_security_group.deployment_controller.id]
  disable_api_termination = false
  key_name = var.ssh_key_name
  monitoring = true
  subnet_id = random_shuffle.public_subnets.result[0]
  associate_public_ip_address = true
  iam_instance_profile = var.iam_instance_profile

  tags = {
    Name = "AB2D-${upper(var.env)}-DEPLOYMENT-CONTROLLER"
    application = "AB2D"
    stack = var.env
    purpose = "ECS container instance"
    sensitivity = "Public"
    maintainer = "lonnie.hanekamp@semanticbits.com"
    cpm_backup = "NoBackup"
    purchase_type = "On-Demand"
    os_license = "Red Hat Enterprise Linux"
    gold_disk_name = var.gold_disk_name
    business = "CMS"
  }

}

resource "aws_eip" "deployment_controller" {
  instance = aws_instance.deployment_controller.id
  vpc = true
}

resource "null_resource" "wait" {
  depends_on = ["aws_instance.deployment_controller","aws_eip.deployment_controller"]
  triggers = {controller_id = aws_instance.deployment_controller.id}

  provisioner "local-exec" {
    command = "sleep 120"
  }
}

resource "null_resource" "list-api-instances-script" {
  depends_on = ["null_resource.wait"]
  triggers = {controller_id = aws_instance.deployment_controller.id}

  provisioner "local-exec" {
    command = "scp -i ~/.ssh/${var.ssh_key_name}.pem ${path.cwd}/../../environments/cms-ab2d-${var.env}/list-api-instances.sh ${var.linux_user}@${aws_eip.deployment_controller.public_ip}:/home/${var.linux_user}"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.deployment_controller.public_ip} 'chmod +x /home/${var.linux_user}/list-api-instances.sh'"
  }
}

resource "null_resource" "set-hostname" {
  depends_on = ["null_resource.wait"]
  triggers = {controller_id = aws_instance.deployment_controller.id}

  provisioner "local-exec" {
    command = "ssh -tt -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.deployment_controller.public_ip} 'echo \"ab2d-${var.env}\" > /tmp/hostname && sudo mv /tmp/hostname /etc/hostname && sudo hostname \"ab2d-${var.env}\"'"
  }
}

resource "null_resource" "deployment_contoller_private_key" {
  depends_on = ["null_resource.wait"]
  triggers = {controller_id = aws_instance.deployment_controller.id}
  provisioner "local-exec" {
    command = "scp -i ~/.ssh/${var.ssh_key_name}.pem ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.deployment_controller.public_ip}:/tmp/id.rsa"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.deployment_controller.public_ip} 'chmod 600 /tmp/id.rsa && mv /tmp/id.rsa ~/.ssh/'"
  }
}

resource "null_resource" "ssh_client_config" {
  depends_on = ["null_resource.wait"]
  triggers = {controller_id = aws_instance.deployment_controller.id}
  provisioner "local-exec" {
    command = "scp -i ~/.ssh/${var.ssh_key_name}.pem ../../environments/cms-ab2d-${var.env}/client_config ${var.linux_user}@${aws_eip.deployment_controller.public_ip}:/home/${var.linux_user}/.ssh/config"
  }

  provisioner "local-exec" {
    command = "ssh -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.deployment_controller.public_ip} 'chmod 640 /home/${var.linux_user}/.ssh/config'"
  }
}

resource "null_resource" "remove_docker_from_controller" {
  depends_on = ["null_resource.wait"]
  triggers = {controller_id = aws_instance.deployment_controller.id}
  provisioner "local-exec" {
    command = "ssh -tt -i ~/.ssh/${var.ssh_key_name}.pem ${var.linux_user}@${aws_eip.deployment_controller.public_ip} 'sudo yum -y remove docker-ce-*'"
  }
}

resource "aws_ecs_cluster" "ab2d" {
  name = "ab2d-${var.env}"
}

resource "aws_ecs_task_definition" "api" {
  family = "api"
  container_definitions = <<JSON
  [
    {
      "name": "ab2d-api",
      "image": "${var.docker_repository_url}",
      "essential": true,
      "memory": 2048,
      "portMappings": [
        {
          "containerPort": ${var.container_port},
          "hostPort": ${var.host_port}
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
  name = "ab2d-${var.env}"
  internal = false
  load_balancer_type = "application"
  security_groups = [aws_security_group.api.id]
  subnets = var.controller_subnet_ids
  enable_deletion_protection = true
  enable_cross_zone_load_balancing = true

  access_logs {
    bucket = var.logging_bucket
    prefix = "ab2d-${var.env}"
    enabled = true
  }
}

resource "aws_lb_target_group" "api" {
  name = "ab2d-api-${var.env}"
  port = var.host_port
  protocol = "HTTP"
  vpc_id = var.vpc_id

  health_check {
    healthy_threshold = 5
    unhealthy_threshold = 2
    timeout = 2
    path = "/api"
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
  name = "ab2d-api"
  cluster = aws_ecs_cluster.ab2d.id
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
  name_prefix = "ab2d-${var.env}-"
  image_id = var.ami_id
  instance_type = var.instance_type
  iam_instance_profile = var.iam_instance_profile
  key_name = var.ssh_key_name
  security_groups = [aws_security_group.api.id]  
  user_data = templatefile("${path.module}/userdata.tpl",{ env = var.env, cluster_name = "ab2d-${var.env}" })
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
      value = "AB2D-API-${upper(var.env)}"
      propagate_at_launch = true
    },
    {
      key = "application"
      value = "AB2D"
      propagate_at_launch = true
    },
    {
      key = "stack"
      value = var.env
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
