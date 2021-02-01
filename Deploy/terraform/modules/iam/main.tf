#
# Create assume role that will be used by management account for automation
#

#
# Create instance role
#

data "aws_iam_policy" "amazon_ec2_container_service_for_ec2_role" {
  arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

# Create Ab2dPackerPolicy

data "aws_iam_policy_document" "instance_role_packer_policy" {
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

resource "aws_iam_policy" "packer_policy" {
  name   = "Ab2dPackerV2Policy"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.instance_role_packer_policy.json
}

# Create Ab2dS3AccessPolicy

data "aws_iam_policy_document" "instance_role_s3_access_policy" {
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
      "arn:aws:s3:::${var.env}-automation/*",
      "arn:aws:s3:::${var.env}-cloudtrail/*",
      "arn:aws:s3:::${var.env}/*"
    ]
  }
}

resource "aws_iam_policy" "s3_access_policy" {
  name   = "Ab2dS3AccessV2Policy"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.instance_role_s3_access_policy.json
}

# Create Ab2dCloudWatchLogsPolicy

data "aws_iam_policy_document" "instance_role_cloud_watch_logs_policy" {
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

resource "aws_iam_policy" "cloud_watch_logs_policy" {
  name   = "Ab2dCloudWatchLogsV2Policy"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.instance_role_cloud_watch_logs_policy.json
}

# Create Ab2dInstanceRole

data "aws_iam_policy_document" "instance_role_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = [
        "ec2.amazonaws.com",
        "ecs-tasks.amazonaws.com",
        "s3.amazonaws.com",
        "vpc-flow-logs.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role" "ab2d_instance_role" {
  name               = "Ab2dInstanceV2Role"
  path               = "/delegatedadmin/developer/"
  assume_role_policy = data.aws_iam_policy_document.instance_role_assume_role_policy.json
  permissions_boundary = "arn:aws:iam::${var.aws_account_number}:policy/cms-cloud-admin/developer-boundary-policy"
}

resource "aws_iam_role_policy_attachment" "instance_role_packer_policy_attach" {
  role       = aws_iam_role.ab2d_instance_role.name
  policy_arn = aws_iam_policy.packer_policy.arn
}

resource "aws_iam_role_policy_attachment" "instance_role_s3_access_policy_attach" {
  role       = aws_iam_role.ab2d_instance_role.name
  policy_arn = aws_iam_policy.s3_access_policy.arn
}

resource "aws_iam_role_policy_attachment" "instance_role_cloud_watch_logs_policy_attach" {
  role       = aws_iam_role.ab2d_instance_role.name
  policy_arn = aws_iam_policy.cloud_watch_logs_policy.arn
}

resource "aws_iam_role_policy_attachment" "amazon_ec2_container_service_for_ec2_role_attach" {
  role       = aws_iam_role.ab2d_instance_role.name
  policy_arn = data.aws_iam_policy.amazon_ec2_container_service_for_ec2_role.arn
}

resource "aws_iam_instance_profile" "ab2d_instance_profile" {
  name = "Ab2dInstanceV2Profile"
  path = "/delegatedadmin/developer/"
  role = aws_iam_role.ab2d_instance_role.name
}
