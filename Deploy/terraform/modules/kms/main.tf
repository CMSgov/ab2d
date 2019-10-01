resource "aws_kms_key" "a" {
  description = "KMS-AB2D-${upper(var.env)}"
  tags = {
    Name = "KMS-AB2D-${upper(var.env)}"
  }
  policy =<<EOF
{
    "Version": "2012-10-17",
    "Id": "key-default-1",
    "Statement": [
        {
            "Sid": "Enable IAM User Permissions",
            "Effect": "Allow",
            "Principal": {
                "AWS": [
                    "arn:aws:iam::626512334475:root",
                    "arn:aws:iam::626512334475:role/AB2D",
                    "arn:aws:iam::626512334475:user/HV7K"
                ]
            },
            "Action": "kms:*",
            "Resource": "*"
        }
    ]
}
EOF
}

resource "aws_kms_alias" "a" {
  name = "alias/KMS-AB2D-${upper(var.env)}"
  target_key_id = aws_kms_key.a.key_id
}
