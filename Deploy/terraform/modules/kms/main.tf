resource "aws_kms_key" "a" {
  description = "ab2d-${lower(var.env)}-kms"
  tags = {
    Name = "ab2d-${lower(var.env)}-kms"
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
                    "arn:aws:iam::${var.aws_account_number}:root",
                    "arn:aws:iam::${var.aws_account_number}:role/Ab2dInstanceRole",
                    "arn:aws:iam::${var.aws_account_number}:user/lonnie.hanekamp@semanticbits.com"
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
  name = "alias/ab2d-${lower(var.env)}-kms"
  target_key_id = aws_kms_key.a.key_id
}
