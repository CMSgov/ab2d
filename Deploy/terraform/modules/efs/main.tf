resource "aws_efs_file_system" "efs" {
  creation_token = "${lower(var.env)}-efs"
  encrypted      = "true"
  kms_key_id     = var.encryption_key_arn
  tags = {
    Name = "${lower(var.env)}-efs"
  }
}

resource "aws_security_group" "efs" {
  name        = "${lower(var.env)}-efs-sg"
  description = "EFS"
  vpc_id      = var.vpc_id
  tags = {
    Name = "${lower(var.env)}-efs-sg"
  }
}
