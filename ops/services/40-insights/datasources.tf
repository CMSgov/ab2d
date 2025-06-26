data "aws_db_instance" "this" {
  db_instance_identifier = local.service_prefix
}

resource "aws_quicksight_data_source" "rds" {
  data_source_id = "${local.app}-${local.env}-rds"
  name           = "${local.app}-${local.env}-rds"
  type           = "POSTGRESQL"

  credentials {
    credential_pair {
      username = local.db_username
      password = local.db_password
    }
  }

  parameters {
    rds {
      database    = local.db_name
      instance_id = data.aws_db_instance.this.id
    }
  }

  dynamic "permission" {
    for_each = local.data_admins
    content {
      actions = [
        "quicksight:DeleteDataSource",
        "quicksight:DescribeDataSource",
        "quicksight:DescribeDataSourcePermissions",
        "quicksight:PassDataSource",
        "quicksight:UpdateDataSource",
        "quicksight:UpdateDataSourcePermissions",
      ]
      principal = "arn:aws:quicksight:us-east-1:${local.aws_account_id}:${permission.value}"
    }
  }

  ssl_properties {
    disable_ssl = false
  }

  vpc_connection_properties {
    vpc_connection_arn = aws_quicksight_vpc_connection.this.arn
  }
}
