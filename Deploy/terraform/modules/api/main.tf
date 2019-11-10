#
# TEMPORARILY COMMENTED OUT BEGIN
#

# # LSH SKIP FOR NOW BEGIN
# # vpn-private-sec-group-id      = var.vpn-private-sec-group-id
# # enterprise-tools-sec-group-id = var.enterprise-tools-sec-group-id
# # LSH SKIP FOR NOW END
# module "controller" {
#   source                = "../../modules/controller"
#   env                   = var.env
#   vpc_id                = var.vpc_id
#   controller_subnet_ids = var.controller_subnet_ids
#   db_sec_group_id       = module.db.aws_security_group_sg_database_id
#   ami_id                = var.ami_id
#   instance_type         = var.instance_type
#   linux_user            = var.linux_user
#   ssh_key_name          = var.ssh_key_name
#   iam_instance_profile  = var.iam_instance_profile
#   gold_disk_name        = var.gold_disk_name
# }

# resource "aws_security_group" "api" {
#   name        = "ab2d-${lower(var.env)}-api-sg"
#   description = "API security group"
#   vpc_id      = var.vpc_id
#   tags = {
#     Name = "ab2d-${lower(var.env)}-api-sg"
#   }
# }

# resource "aws_security_group_rule" "node_access" {
#   type        = "ingress"
#   description = "Node Access"
#   from_port   = "-1"
#   to_port     = "-1"
#   protocol    = "-1"
#   source_security_group_id = aws_security_group.api.id
#   security_group_id = module.controller.deployment_controller_sec_group_id
# }

# resource "aws_security_group_rule" "host_port" {
#   type        = "ingress"
#   description = "Host Port"
#   from_port   = var.host_port
#   to_port     = var.host_port
#   protocol    = "tcp"
#   source_security_group_id = aws_security_group.api.id
#   security_group_id = aws_security_group.api.id
# }

# resource "aws_security_group_rule" "controller_access" {
#   type        = "ingress"
#   description = "Controller Access"
#   from_port   = "-1"
#   to_port     = "-1"
#   protocol    = "-1"
#   source_security_group_id = module.controller.deployment_controller_sec_group_id
#   security_group_id = aws_security_group.api.id
# }

# # LSH SKIP FOR NOW BEGIN
# # resource "aws_security_group_rule" "vpn_http" {
# #   type        = "ingress"
# #   description = "VPN Access"
# #   from_port   = var.host_port
# #   to_port     = var.host_port
# #   protocol    = "tcp"
# #   cidr_blocks = ["10.252.0.0/16", "10.232.32.0/19", "10.251.0.0/16", "52.20.26.200/32", "34.196.35.156/32"]
# #   security_group_id = aws_security_group.api.id
# # }
# # LSH SKIP FOR NOW END

# # LSH SKIP FOR NOW BEGIN
# # resource "aws_security_group_rule" "vpn_https" {
# #   type        = "ingress"
# #   description = "VPN Access"
# #   from_port   = "443"
# #   to_port     = "443"
# #   protocol    = "tcp"
# #   cidr_blocks = ["10.252.0.0/16", "10.232.32.0/19", "10.251.0.0/16", "52.20.26.200/32", "34.196.35.156/32"]
# #   security_group_id = aws_security_group.api.id
# # }
# # LSH SKIP FOR NOW END

# # LSH SKIP FOR NOW BEGIN
# # resource "aws_security_group_rule" "kong_http" {
# #   type        = "ingress"
# #   description = "Kong"
# #   from_port   = var.host_port
# #   to_port     = var.host_port
# #   protocol    = "tcp"
# #   cidr_blocks = ["34.204.33.165/32","34.226.82.144/32","34.200.65.22/32","34.227.6.19/32"]
# #   security_group_id = aws_security_group.api.id
# # }
# # LSH SKIP FOR NOW END

# # LSH SKIP FOR NOW BEGIN
# # resource "aws_security_group_rule" "kong_https" {
# #   type        = "ingress"
# #   description = "Kong"
# #   from_port   = "443"
# #   to_port     = "443"
# #   protocol    = "tcp"
# #   cidr_blocks = ["34.204.33.165/32","34.226.82.144/32","34.200.65.22/32","34.227.6.19/32"]
# #   security_group_id = aws_security_group.api.id
# # }
# # LSH SKIP FOR NOW END

# resource "aws_security_group_rule" "egress_api" {
#   type        = "egress"
#   description = "Allow all egress"
#   from_port   = "0"
#   to_port     = "0"
#   protocol    = "-1"
#   cidr_blocks = ["0.0.0.0/0"]
#   security_group_id = aws_security_group.api.id
# }

# resource "aws_security_group_rule" "db_access_api" {
#   type        = "ingress"
#   description = "App connections"
#   from_port   = "5432"
#   to_port     = "5432"
#   protocol    = "tcp"
#   source_security_group_id = aws_security_group.api.id
#   security_group_id = var.db_sec_group_id
# }

# resource "aws_ecs_cluster" "ab2d" {
#   name = "ab2d-${lower(var.env)}"
# }

# resource "aws_ecs_task_definition" "api" {
#   family = "api"
#   container_definitions = <<JSON
#   [
#     {
#       "name": "ab2d-api",
#       "image": "${var.docker_repository_url}",
#       "essential": true,
#       "memory": 2048,
#       "portMappings": [
#         {
#           "containerPort": ${var.container_port},
#           "hostPort": ${var.host_port}
#         }
#       ],
#       "logConfiguration": {
#         "logDriver": "syslog"
#       },
#       "healthCheck": null
#     }
#   ]
# JSON
#   requires_compatibilities = ["EC2"]
#   network_mode = "bridge"
#   cpu = 1024
#   memory = 2048
#   execution_role_arn = "arn:aws:iam::${var.aws_account_number}:role/Ab2dInstanceRole"
# }

# resource "aws_lb" "api" {
#   name = "ab2d-${lower(var.env)}"
#   internal = false
#   load_balancer_type = "application"
#   security_groups = [aws_security_group.api.id]
#   subnets = var.controller_subnet_ids
#   enable_deletion_protection = true
#   enable_cross_zone_load_balancing = true

#   access_logs {
#     bucket = var.logging_bucket
#     prefix = "ab2d-${lower(var.env)}"
#     enabled = true
#   }
# }

# resource "aws_lb_target_group" "api" {
#   name = "ab2d-${lower(var.env)}-api-tg"
#   port = var.host_port
#   protocol = "HTTP"
#   vpc_id = var.vpc_id

#   health_check {
#     healthy_threshold = 5
#     unhealthy_threshold = 2
#     timeout = 2
#     path = "/api"
#     interval = 5
#   }
# }

# resource "aws_lb_listener" "api" {
#   load_balancer_arn = aws_lb.api.arn
#   port = var.host_port
#   protocol = "HTTP"

#   default_action {
#     target_group_arn = aws_lb_target_group.api.arn
#     type = "forward"
#   }
# }

# resource "aws_ecs_service" "api" {
#   depends_on = ["aws_lb.api"]
#   name = "ab2d-api"
#   cluster = aws_ecs_cluster.ab2d.id
#   task_definition = var.override_task_definition_arn != "" ? var.override_task_definition_arn : aws_ecs_task_definition.api.arn
#   desired_count = 5
#   launch_type = "EC2"
#   scheduling_strategy = "DAEMON"

#   load_balancer {
#     target_group_arn = aws_lb_target_group.api.arn
#     container_name = "ab2d-api"
#     container_port = var.container_port
#   }
# }

# # LSH SKIP FOR NOW BEGIN
# # security_groups = [aws_security_group.api.id,var.enterprise-tools-sec-group-id,var.vpn-private-sec-group-id]
# # LSH SKIP FOR NOW BEGIN
# resource "aws_launch_configuration" "launch_config" {
#   name_prefix = "ab2d-${lower(var.env)}-"
#   image_id = var.ami_id
#   instance_type = var.instance_type
#   iam_instance_profile = var.iam_instance_profile
#   key_name = var.ssh_key_name
#   security_groups = [aws_security_group.api.id]  
#   user_data = templatefile("${path.module}/userdata.tpl",{ env = "${lower(var.env)}", cluster_name = "ab2d-${lower(var.env)}" })
#   lifecycle { create_before_destroy = true }
# }

# resource "aws_autoscaling_group" "asg" {
#   depends_on = ["aws_launch_configuration.launch_config"]
#   availability_zones = ["us-east-1a","us-east-1b","us-east-1c","us-east-1d","us-east-1e"]
#   name = aws_launch_configuration.launch_config.name
#   max_size = var.max_instances
#   min_size = var.min_instances
#   desired_capacity = var.desired_instances
#   health_check_type = "EC2"
#   launch_configuration = aws_launch_configuration.launch_config.name
#   target_group_arns = [aws_lb_target_group.api.arn]
#   enabled_metrics = ["GroupTerminatingInstances", "GroupInServiceInstances", "GroupMaxSize", "GroupTotalInstances", "GroupMinSize", "GroupPendingInstances", "GroupDesiredCapacity", "GroupStandbyInstances"]
#   wait_for_elb_capacity = var.autoscale_group_wait
#   vpc_zone_identifier = var.node_subnet_ids
#   lifecycle { create_before_destroy = true }

#   tags = [
#     {
#       key = "Name"
#       value = "ab2d-${lower(var.env)}-api"
#       propagate_at_launch = true
#     },
#     {
#       key = "application"
#       value = "ab2d"
#       propagate_at_launch = true
#     },
#     {
#       key = "stack"
#       value = "${lower(var.env)}"
#       propagate_at_launch = true
#     },
#     {
#       key = "purpose"
#       value = "ECS container instance"
#       propagate_at_launch = true
#     },
#     {
#       key = "sensitivity"
#       value = "Public"
#       propagate_at_launch = true
#     },
#     {
#       key = "maintainer"
#       value = "lonnie.hanekamp@semanticbits.com"
#       propagate_at_launch = true
#     },
#     {
#       key = "cpm backup"
#       value = "NoBackup"
#       propagate_at_launch = true
#     },
#     {
#       key = "purchase_type"
#       value = "On-Demand"
#       propagate_at_launch = true
#     },
#     {
#       key = "os_license"
#       value = "Red Hat Enterprise Linux"
#       propagate_at_launch = true
#     },
#     {
#       key = "gold_disk_name"
#       value = var.gold_disk_name
#       propagate_at_launch = true
#     },
#     {
#       key = "business"
#       value = "CMS"
#       propagate_at_launch = true
#     }
#   ]
# }

# resource "aws_autoscaling_policy" "percent_capacity" {
#   name = "percent_capacity"
#   scaling_adjustment = var.percent_capacity_increase
#   adjustment_type = "PercentChangeInCapacity"
#   cooldown = 300
#   autoscaling_group_name = aws_autoscaling_group.asg.name
# }


# resource "aws_autoscaling_policy" "target_cpu" {
#   name = "target_CPU"
#   autoscaling_group_name = aws_autoscaling_group.asg.name
#   policy_type = "TargetTrackingScaling"

#   target_tracking_configuration {
#     predefined_metric_specification {
#       predefined_metric_type = "ASGAverageCPUUtilization"
#     }
#     target_value = 80.0
#   }
# }

#
# TEMPORARILY COMMENTED OUT END
#
