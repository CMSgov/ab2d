resource "aws_efs_file_system" "efs" {
  creation_token = "cms-ab2d"
  encrypted      = "true"
  kms_key_id     = var.encryption_key_arn
  tags = {
    Name = "AB2D-${upper(var.env)}-EFS"
  }
}
