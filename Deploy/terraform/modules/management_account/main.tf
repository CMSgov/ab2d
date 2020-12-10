# Create instance role for the management account

data "aws_iam_policy_document" "instance_role_assume_role_policy" {
  statement {
    sid = "1"

    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type        = "Service"
      identifiers = [
        "ec2.amazonaws.com",
	"s3.amazonaws.com",
	"vpc-flow-logs.amazonaws.com",
	"ecs-tasks.amazonaws.com"
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

# Create instance profile for instance role

resource "aws_iam_instance_profile" "ab2d_instance_profile" {
  name = "Ab2dInstanceV2Profile"
  path = "/delegatedadmin/developer/"
  role = aws_iam_role.ab2d_instance_role.name
}

# Create assume policy

data "aws_iam_policy_document" "allow_assume_role_in_target_account_policy" {
  statement {
    sid = "1"

    actions = [
      "sts:AssumeRole"
    ]

    resources = var.mgmt_target_aws_account_mgmt_roles
  }
}

resource "aws_iam_policy" "assume_policy" {
  name   = "Ab2dAssumeV2Policy"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.allow_assume_role_in_target_account_policy.json
}

# Attach assume policy to instance role

resource "aws_iam_role_policy_attachment" "instance_role_assume_policy_attach" {
  role       = aws_iam_role.ab2d_instance_role.name
  policy_arn = aws_iam_policy.assume_policy.arn
}

# Create CloudWatch Logs policy

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

# Attach CloudWatch Logs policy to instance role

resource "aws_iam_role_policy_attachment" "instance_role_cloud_watch_logs_policy_attach" {
  role       = aws_iam_role.ab2d_instance_role.name
  policy_arn = aws_iam_policy.cloud_watch_logs_policy.arn
}

# Create Packer policy

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

# Attach Packer policy to instance role

resource "aws_iam_role_policy_attachment" "instance_role_packer_policy_attach" {
  role       = aws_iam_role.ab2d_instance_role.name
  policy_arn = aws_iam_policy.packer_policy.arn
}

# Create S3 policy

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

# Attach S3 policy to instance role

resource "aws_iam_role_policy_attachment" "instance_role_s3_access_policy_attach" {
  role       = aws_iam_role.ab2d_instance_role.name
  policy_arn = aws_iam_policy.s3_access_policy.arn
}

# Create KMS policy

data "aws_kms_alias" "kms_alias" {
  name = "alias/ab2d-kms"
}

data "aws_iam_policy_document" "instance_role_kms_policy" {
  statement {
    actions = [
      "kms:Decrypt"
    ]

    resources = [
      data.aws_kms_alias.kms_alias.target_key_arn
    ]
  }
}

resource "aws_iam_policy" "kms_policy" {
  name   = "Ab2dKmsV2Policy"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.instance_role_kms_policy.json
}

# Attach KMS policy to instance role

resource "aws_iam_role_policy_attachment" "instance_role_kms_policy_attach" {
  role       = aws_iam_role.ab2d_instance_role.name
  policy_arn = aws_iam_policy.kms_policy.arn
}

# Create management role for the management account

data "aws_iam_policy_document" "allow_assume_role_in_mgmt_account_policy" {
  statement {
    sid = "1"

    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type        = "AWS"
      identifiers = [
        "arn:aws:iam::${var.mgmt_aws_account_number}:role/ct-ado-ab2d-application-admin"
      ]
    }
  }
}

resource "aws_iam_role" "ab2d_mgmt_role" {
  name               = "Ab2dMgmtV2Role"
  path               = "/delegatedadmin/developer/"
  assume_role_policy = data.aws_iam_policy_document.allow_assume_role_in_mgmt_account_policy.json
  permissions_boundary = "arn:aws:iam::${var.aws_account_number}:policy/cms-cloud-admin/developer-boundary-policy"
}

# Create lambda role for the management account

data "aws_iam_policy_document" "lambda_ec2_policy" {

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

  statement {
    actions = [
      "ec2:Start*",
      "ec2:Stop*",
      "ec2:Describe*"
    ]

    resources = [
      "*"
    ]
  }
}

resource "aws_iam_policy" "lambda_ec2_policy" {
  name   = "Ab2dLambdaEc2Policy"
  path   = "/delegatedadmin/developer/"
  policy = data.aws_iam_policy_document.lambda_ec2_policy.json
}

data "aws_iam_policy_document" "lambda_role_assume_role_policy" {
  statement {
    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type        = "Service"
      identifiers = [
        "lambda.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role" "ab2d_lambda_role" {
  name               = "Ab2dLambdaRole"
  path               = "/delegatedadmin/developer/"
  assume_role_policy = data.aws_iam_policy_document.lambda_role_assume_role_policy.json
  permissions_boundary = "arn:aws:iam::${var.aws_account_number}:policy/cms-cloud-admin/developer-boundary-policy"
}

resource "aws_iam_role_policy_attachment" "lambda_role_lambda_ec2_policy_attach" {
  role       = aws_iam_role.ab2d_lambda_role.name
  policy_arn = aws_iam_policy.lambda_ec2_policy.arn
}
