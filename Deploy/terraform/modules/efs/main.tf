resource "aws_efs_file_system" "efs" {
  creation_token = "ab2d-${lower(var.env)}-efs"
  encrypted      = "true"
  kms_key_id     = var.encryption_key_arn
  tags = {
    Name = "ab2d-${lower(var.env)}-efs"
  }
}