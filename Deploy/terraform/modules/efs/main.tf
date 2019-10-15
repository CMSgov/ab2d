resource "aws_efs_file_system" "efs" {
  creation_token = "cms-ab2d"
  encrypted      = "true"
  kms_key_id     = var.encryption_key_arn
  tags = {
    Name = "AB2D-${upper(var.env)}-EFS"
  }
}

resource "aws_efs_mount_target" "alpha" {
  file_system_id = "${aws_efs_file_system.efs.id}"
  subnet_id      = var.alpha
}

resource "aws_efs_mount_target" "beta" {
  file_system_id = "${aws_efs_file_system.efs.id}"
  subnet_id      = var.beta
}

resource "aws_efs_mount_target" "gamma" {
  file_system_id = "${aws_efs_file_system.efs.id}"
  subnet_id      = var.gamma
}
