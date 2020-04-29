variable "mgmt_aws_account_number" {}
variable "ab2d_mgmt_role_arn" {}

# variable "mgmt_aws_account_number" {}
# variable "aws_account_number" {}
# variable "env" {}

# Create "Ab2dInstanceRole" DONE

# - Create and attach "Ab2dCloudWatchLogsPolicy" DONE
# - Create "Ab2dKmsPolicy" DONE
# - Create "Ab2dS3AccessPolicy" DONE
# - Create "Ab2dPackerPolicy" DONE
# - Attach "CMSApprovedAWSServices" TO DO
# - Attach "AmazonEC2ContainerServiceforEC2Role" TO DO

# - Create "Ab2dBfdProdSbxPolicy" COMMENTED OUT

# Create "Ab2dPermissionToPassRolesPolicy" SKIP
# Create "Ab2dAccessPolicy" SKIP
# Create "Ab2dAssumePolicy" SKIP



# data "aws_iam_policy_document" "instance_role_zzz_policy" {
#   statement {
#     actions = [
#     ]

#     resources = [
#       "*"
#     ]
#   }
# }

# resource "aws_iam_policy" "zzz_policy" {
#   name   = "Ab2dZzzPolicy"
#   policy = "${data.aws_iam_policy_document.instance_role_zzz_policy.json}"
# }


# {
#   "Version": "2012-10-17",
#   "Statement": [
#     {
#       "Effect": "Allow",
#       "Principal": {
#         "AWS": "arn:aws:iam::653916833532:root"
#       },
#       "Action": "sts:AssumeRole",
#       "Condition": {}
#     }
#   ]
# }

#
# arn:aws:iam::595094747606:role/Ab2dMgmtRole
#

# arn:aws:iam::595094747606:policy/CMSApprovedAWSServices