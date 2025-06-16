data "aws_iam_policy" "cms_cloud_ssm_iam" {
  arn = "arn:aws:iam::${local.aws_account_number}:policy/cms-cloud-ssm-iam-policy-v3"
}

data "aws_iam_policy" "developer_boundary_policy" {
  name = "developer-boundary-policy"
}

# WORKER
resource "aws_iam_role" "worker" {
  name                 = "${local.service_prefix}-worker"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = data.aws_iam_policy.developer_boundary_policy.arn
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = [
            "vpc-flow-logs.amazonaws.com",
            "s3.amazonaws.com",
            "ec2.amazonaws.com",
            "ecs-tasks.amazonaws.com"
          ]
        }
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "worker" {
  for_each = {
    a = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role",
    b = "arn:aws:iam::aws:policy/AmazonECS_FullAccess",
    c = "arn:aws:iam::aws:policy/AmazonAPIGatewayInvokeFullAccess",
    d = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    e = "arn:aws:iam::aws:policy/AmazonS3FullAccess",
    f = "arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser",
    g = "arn:aws:iam::aws:policy/SecretsManagerReadWrite",
    h = "arn:aws:iam::aws:policy/EC2InstanceProfileForImageBuilderECRContainerBuilds",
    i = "arn:aws:iam::aws:policy/AmazonSQSFullAccess",
    j = "arn:aws:iam::aws:policy/AmazonSNSFullAccess",
    k = "arn:aws:iam::aws:policy/AmazonEC2FullAccess",
    l = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
    m = data.aws_iam_policy.cms_cloud_ssm_iam.arn
    n = aws_iam_policy.microservices.arn
  }

  role       = aws_iam_role.worker.name
  policy_arn = each.value
}

# API
resource "aws_iam_role" "api" {
  permissions_boundary = data.aws_iam_policy.developer_boundary_policy.arn
  name                 = "${local.service_prefix}-api"
  path                 = "/delegatedadmin/developer/"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = [
            "vpc-flow-logs.amazonaws.com",
            "s3.amazonaws.com",
            "ec2.amazonaws.com",
            "ecs-tasks.amazonaws.com"
          ]
        }
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "api" {
  for_each = {
    a = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role",
    b = "arn:aws:iam::aws:policy/AmazonECS_FullAccess",
    c = "arn:aws:iam::aws:policy/AmazonAPIGatewayInvokeFullAccess",
    d = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
    e = "arn:aws:iam::aws:policy/AmazonS3FullAccess",
    f = "arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser",
    g = "arn:aws:iam::aws:policy/SecretsManagerReadWrite",
    h = "arn:aws:iam::aws:policy/EC2InstanceProfileForImageBuilderECRContainerBuilds",
    i = "arn:aws:iam::aws:policy/AmazonSQSFullAccess",
    j = "arn:aws:iam::aws:policy/AmazonEC2FullAccess",
    k = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
    l = data.aws_iam_policy.cms_cloud_ssm_iam.arn,
    m = aws_iam_policy.microservices.arn
  }

  role       = aws_iam_role.api.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "microservices" {
  statement {
    actions = [
      "kms:*"
    ]
    resources = [
      local.env_key_alias.target_key_arn
    ]
  }
  statement {
    actions = [
      "ssm:GetParametersByPath",
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = [
      "arn:aws:ssm:${local.region_name}:${local.aws_account_number}:parameter/ab2d/${local.env}/*"
    ]
  }
}

resource "aws_iam_policy" "microservices" {
  name = "${local.service_prefix}-microservices-kms"
  path = "/delegatedadmin/developer/"

  policy = data.aws_iam_policy_document.microservices.json
}

# MICROSERVICES
resource "aws_iam_role" "microservices" {
  #TODO consider simplifying the human readable name
  name                 = "${local.service_prefix}-microservices"
  path                 = "/delegatedadmin/developer/"
  permissions_boundary = data.aws_iam_policy.developer_boundary_policy.arn

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = [
            "ecs-tasks.amazonaws.com",
            "ec2.amazonaws.com",
            "s3.amazonaws.com",
            "chatbot.amazonaws.com",
            "lambda.amazonaws.com"
          ]
        }
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "microservices" {
  for_each = {
    a = "arn:aws:iam::aws:policy/AmazonSQSFullAccess"
    b = "arn:aws:iam::aws:policy/SecretsManagerReadWrite"
    c = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
    d = "arn:aws:iam::aws:policy/AWSKeyManagementServicePowerUser"
    e = "arn:aws:iam::aws:policy/AmazonEC2FullAccess"
    f = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
    g = "arn:aws:iam::aws:policy/AmazonECS_FullAccess"
    h = "arn:aws:iam::aws:policy/EC2InstanceProfileForImageBuilderECRContainerBuilds"
    i = "arn:aws:iam::aws:policy/ElasticLoadBalancingFullAccess"
    j = aws_iam_policy.microservices.arn
  }

  role       = aws_iam_role.microservices.name
  policy_arn = each.value
}

# Create instance role
data "aws_iam_policy_document" "instance_role_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type = "Service"
      identifiers = [
        "ec2.amazonaws.com",
        "ecs-tasks.amazonaws.com",
        "s3.amazonaws.com",
        "vpc-flow-logs.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role" "ab2d_instance" {
  name                 = "${local.service_prefix}-instance-role"
  path                 = "/delegatedadmin/developer/"
  assume_role_policy   = data.aws_iam_policy_document.instance_role_assume_role.json
  permissions_boundary = "arn:aws:iam::${local.aws_account_number}:policy/cms-cloud-admin/developer-boundary-policy"
}

# Reference AmazonEC2ContainerServiceforEC2Role
data "aws_iam_policy" "amazon_ec2_container_service_for_ec2" {
  arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

# Create KMS policy
resource "aws_iam_policy" "kms" {
  name   = "${local.service_prefix}-kms"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.kms.json
}

# Create packer policy
data "aws_iam_policy_document" "instance_role_packer" {
  statement {
    actions = [
      "ec2:AttachVolume",
      "ec2:AuthorizeSecurityGroupIngress",
      "ec2:CopyImage",
      "ec2:CreateImage",
      "ec2:CreateKeypair",
      "ec2:CreateSecurityGroup",
      "ec2:CreateSnapshot",
      "ec2:CreateTags",
      "ec2:CreateVolume",
      "ec2:DeleteKeypair",
      "ec2:DeleteSecurityGroup",
      "ec2:DeleteSnapshot",
      "ec2:DeleteVolume",
      "ec2:DeregisterImage",
      "ec2:DescribeImageAttribute",
      "ec2:DescribeImages",
      "ec2:DescribeInstances",
      "ec2:DescribeRegions",
      "ec2:DescribeSecurityGroups",
      "ec2:DescribeSnapshots",
      "ec2:DescribeSubnets",
      "ec2:DescribeTags",
      "ec2:DescribeVolumes",
      "ec2:DetachVolume",
      "ec2:GetPasswordData",
      "ec2:ModifyImageAttribute",
      "ec2:ModifyInstanceAttribute",
      "ec2:ModifySnapshotAttribute",
      "ec2:RegisterImage",
      "ec2:RunInstances",
      "ec2:StopInstances",
      "ec2:TerminateInstances"
    ]

    resources = [
      "*"
    ]
  }
}

resource "aws_iam_policy" "packer" {
  name   = "${local.service_prefix}-packer-policy"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.instance_role_packer.json
}

#FIXME this policy is unfocused and likely over-privileged for greenfield. this needs to be addressed prior to a production/sensitive-environment deployment
# Create S3 access policy
data "aws_iam_policy_document" "instance_role_s3_access" {
  statement {
    actions = [
      "s3:Get*",
      "s3:List*"
    ]

    resources = [
      "*"
    ]
  }

  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      module.platform.logging_bucket.arn,
      "${module.platform.logging_bucket.arn}/*"
    ]
  }

  statement {
    sid = "SQSAccess"
    actions = [
      "sqs:*"
    ]

    resources = [
      aws_sqs_queue.this.arn
    ]
  }

  statement {
    sid = "SNSAccess"
    actions = [
      "SNS:CreateTopic",
      "SNS:Subscribe",
      "SNS:Unsubscribe",
      "SNS:Publish"
    ]
    resources = [
      "*"
    ]
  }

  statement {
    sid = "SecreteManagerPermissions"
    actions = [
      "secretsmanager:*"

    ]
    resources = [
      "*"
    ]
  }
}

resource "aws_iam_policy" "s3_access" {
  name   = "${local.service_prefix}-s3-access-policy"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.instance_role_s3_access.json
}

# Create CloudWatch logs policy
data "aws_iam_policy_document" "instance_role_cloud_watch_logs" {
  statement {
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:DescribeLogGroups",
      "logs:DescribeLogStreams"
    ]

    resources = [
      "arn:aws:logs:*:*:*"
    ]
  }
}

resource "aws_iam_policy" "cloud_watch_logs" {
  name   = "${local.service_prefix}-cloudwatch-logs-policy"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.instance_role_cloud_watch_logs.json
}

# Create Ab2dSsmPolicy
data "aws_iam_policy_document" "instance_role_ssm" {

  statement {
    actions = [
      "ssm:GetParameters"
    ]

    resources = [
      "arn:aws:ssm:*:*:parameter/aws/service/ecs*"
    ]
  }
}

data "aws_iam_policy_document" "kms" {
  statement {
    sid = "AllowEnvCMKAccess"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey",
      "kms:ReEncryptFrom",
      "kms:ReEncryptTo",
      "kms:DescribeKey",
      "kms:CreateGrant",
      "kms:ListGrants",
      "kms:RevokeGrant"
    ]
    resources = [local.env_key_alias.target_key_arn]
  }
}

resource "aws_iam_policy" "ssm" {
  name   = "${local.service_prefix}-ssm"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.instance_role_ssm.json
}

# Attach policies to Instance Role
resource "aws_iam_role_policy_attachment" "instance_role_kms" {
  role       = aws_iam_role.ab2d_instance.name
  policy_arn = aws_iam_policy.kms.arn
}

resource "aws_iam_role_policy_attachment" "instance_role_packer" {
  role       = aws_iam_role.ab2d_instance.name
  policy_arn = aws_iam_policy.packer.arn
}

resource "aws_iam_role_policy_attachment" "instance_role_s3_access" {
  role       = aws_iam_role.ab2d_instance.name
  policy_arn = aws_iam_policy.s3_access.arn
}

resource "aws_iam_role_policy_attachment" "instance_role_cloud_watch_logs" {
  role       = aws_iam_role.ab2d_instance.name
  policy_arn = aws_iam_policy.cloud_watch_logs.arn
}

resource "aws_iam_role_policy_attachment" "instance_role_ssm" {
  role       = aws_iam_role.ab2d_instance.name
  policy_arn = aws_iam_policy.ssm.arn
}

resource "aws_iam_role_policy_attachment" "amazon_ec2_container_service_for_ec2" {
  role       = aws_iam_role.ab2d_instance.name
  policy_arn = data.aws_iam_policy.amazon_ec2_container_service_for_ec2.arn
}

#CLDSPT-10122
resource "aws_iam_role_policy_attachment" "instance_role_cms_ssm" {
  role       = aws_iam_role.ab2d_instance.name
  policy_arn = data.aws_iam_policy.cms_cloud_ssm_iam.arn
}

# Create instance profile
resource "aws_iam_instance_profile" "ab2d_instance_profile" {
  name = "${local.service_prefix}-instance-profile"
  path = "/delegatedadmin/developer/"
  role = aws_iam_role.ab2d_instance.name
}


data "aws_iam_policy_document" "kms_key_access" {
  statement {
    sid = "AllowEnvCMKAccess"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey",
      "kms:ReEncrypt",
      "kms:DescribeKey",
      "kms:Encrypt"
    ]
    resources = [local.env_key_alias.target_key_arn]
  }
}

resource "aws_iam_policy" "kms_key_access" {
  name = "${local.service_prefix}-${local.service}-kms-key-access"
  path = "/delegatedadmin/developer/"
  description = "Permissions to access environment ${local.env} KMS CMK"
  policy = data.aws_iam_policy_document.kms_key_access.json
}
