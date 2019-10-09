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
                    "arn:aws:iam::114601554524:root",
                    "arn:aws:iam::114601554524:role/Ab2dInstanceRole",
                    "arn:aws:iam::114601554524:user/lonnie.hanekamp@semanticbits.com"
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
