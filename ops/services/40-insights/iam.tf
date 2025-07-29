#FIXME This probably belongs in the CDAP terraform
resource "aws_iam_role" "this" {
  assume_role_policy = jsonencode(
    {
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "quicksight.amazonaws.com"
          }
        },
      ]
      Version = "2012-10-17"
    }
  )
  force_detach_policies = true
  managed_policy_arns = [                                                      #FIXME: Use a policy attachment resource for these...
    "arn:aws:iam::aws:policy/service-role/AmazonSageMakerQuickSightVPCPolicy", #TODO this might be over-priv'd but, it already exists and it does everything that's needed, I think...
    "arn:aws:iam::aws:policy/service-role/AWSQuickSightListIAM",
    "arn:aws:iam::aws:policy/service-role/AWSQuickSightDescribeRDS"
  ]
  max_session_duration = 3600
  name                 = "cdap-mgmt-quicksight-service-role"
  path                 = "/service-role/"
}
