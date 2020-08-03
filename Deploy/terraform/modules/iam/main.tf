#
# Create assume role that will be used by management account for automation
#

data "aws_iam_policy_document" "mgmt_role_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${var.mgmt_aws_account_number}:root"]
    }
  }
}

data "aws_iam_policy" "cms_approved_aws_services" {
  arn = "arn:aws:iam::${var.aws_account_number}:policy/CMSApprovedAWSServices"
}

resource "aws_iam_role" "ab2d_mgmt_role" {
  name               = "Ab2dMgmtRole"
  assume_role_policy = "${data.aws_iam_policy_document.mgmt_role_assume_role_policy.json}"
}

resource "aws_iam_role_policy_attachment" "cms_approved_aws_services_attach" {
  role       = "${aws_iam_role.ab2d_mgmt_role.name}"
  policy_arn = "${data.aws_iam_policy.cms_approved_aws_services.arn}"
}

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
  name   = "Ab2dPackerPolicy"
  policy = "${data.aws_iam_policy_document.instance_role_packer_policy.json}"
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
      "arn:aws:s3:::${var.env}/*",
      "arn:aws:s3:::ab2d-optout-data-dev/*"
    ]
  }
}

resource "aws_iam_policy" "s3_access_policy" {
  name   = "Ab2dS3AccessPolicy"
  policy = "${data.aws_iam_policy_document.instance_role_s3_access_policy.json}"
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
  name   = "Ab2dCloudWatchLogsPolicy"
  policy = "${data.aws_iam_policy_document.instance_role_cloud_watch_logs_policy.json}"
}

########################
# For Dev, Sbx, and Impl
########################

# # Create Ab2dBfdProdSbxPolicy

# data "aws_iam_policy_document" "instance_role_bfd_prod_sbx_policy" {
#   statement {
#     actions = [
#       "kms:Decrypt",
#       "kms:Encrypt",
#       "kms:DescribeKey",
#       "kms:ReEncrypt*",
#       "kms:GenerateDataKey*"
#     ]

#     resources = [
#       "arn:aws:kms:us-east-1:577373831711:key/20e853ce-f7c6-42f7-b75b-4017b215bd0d"
#     ]
#   }

#   statement {
#     actions = [
#       "s3:GetObject",
#       "s3:ListBucket",
#       "s3:HeadBucket"
#     ]

#     resources = [
#       "arn:aws:s3:::bfd-prod-sbx-medicare-opt-out-577373831711/*",
#       "arn:aws:s3:::bfd-prod-sbx-medicare-opt-out-577373831711"
#     ]
#   }
# }

# resource "aws_iam_policy" "bfd_prod_sbx_policy" {
#   name   = "Ab2dBfdProdSbxPolicy"
#   policy = "${data.aws_iam_policy_document.instance_role_bfd_prod_sbx_policy.json}"
# }

########################

###########################################################################
# Replace Ab2dBfdProdSbxPolicy with Ab2dBfdOptOutPolicy in all environments
###########################################################################

# Create Ab2dBfdOptOutPolicy

data "aws_iam_policy_document" "instance_role_bfd_opt_out_policy" {
  statement {
    sid = "BfdS3BucketPart01"
    
    actions = [
      "s3:GetObject",
      "s3:GetObjectVersion*",
      "s3:ListBucket",
      "s3:ListBucketVersions"
    ]

    resources = [
      "arn:aws:s3:::${var.ab2d_s3_optout_bucket}/*",
      "arn:aws:s3:::${var.ab2d_s3_optout_bucket}"
    ]
  }

  statement {
    sid = "BfdS3BucketPart02"
    
    actions = [
      "s3:HeadBucket"
    ]

    resources = [
      "*"
    ]
  }

  statement {
    sid = "BfdKms"
    
    actions = [
      "kms:Decrypt",
      "kms:DescribeKey",
      "kms:Encrypt",
      "kms:GenerateDataKey*",
      "kms:ReEncrypt*"
    ]

    resources = [
      "${var.bfd_opt_out_kms_arn}"
    ]
  }
}

resource "aws_iam_policy" "bfd_opt_out_policy" {
  name   = "Ab2dBfdOptOutPolicy"
  policy = "${data.aws_iam_policy_document.instance_role_bfd_opt_out_policy.json}"
}

##########

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
  name               = "Ab2dInstanceRole"
  assume_role_policy = "${data.aws_iam_policy_document.instance_role_assume_role_policy.json}"
}

resource "aws_iam_role_policy_attachment" "instance_role_packer_policy_attach" {
  role       = "${aws_iam_role.ab2d_instance_role.name}"
  policy_arn = "${aws_iam_policy.packer_policy.arn}"
}

resource "aws_iam_role_policy_attachment" "instance_role_s3_access_policy_attach" {
  role       = "${aws_iam_role.ab2d_instance_role.name}"
  policy_arn = "${aws_iam_policy.s3_access_policy.arn}"
}

resource "aws_iam_role_policy_attachment" "instance_role_cloud_watch_logs_policy_attach" {
  role       = "${aws_iam_role.ab2d_instance_role.name}"
  policy_arn = "${aws_iam_policy.cloud_watch_logs_policy.arn}"
}

resource "aws_iam_role_policy_attachment" "instance_role_bfd_opt_out_policy_attach" {
  role       = "${aws_iam_role.ab2d_instance_role.name}"
  policy_arn = "${aws_iam_policy.bfd_opt_out_policy.arn}"
}

########################
# For Dev, Sbx, and Impl
########################

# resource "aws_iam_role_policy_attachment" "instance_role_bfd_prod_sbx_policy_attach" {
#   role       = "${aws_iam_role.ab2d_instance_role.name}"
#   policy_arn = "${aws_iam_policy.bfd_prod_sbx_policy.arn}"
# }

########################

resource "aws_iam_role_policy_attachment" "amazon_ec2_container_service_for_ec2_role_attach" {
  role       = "${aws_iam_role.ab2d_instance_role.name}"
  policy_arn = "${data.aws_iam_policy.amazon_ec2_container_service_for_ec2_role.arn}"
}

resource "aws_iam_instance_profile" "test_profile" {
  name = "Ab2dInstanceProfile"
  role = "${aws_iam_role.ab2d_instance_role.name}"
}
