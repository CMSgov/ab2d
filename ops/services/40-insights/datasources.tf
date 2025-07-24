data "aws_rds_cluster" "this" {
  cluster_identifier = local.service_prefix
}

resource "aws_quicksight_data_source" "aurora" {
  data_source_id = "${local.app}-${local.env}-aurora"
  name           = "${local.app}-${local.env}-aurora"
  type           = "AURORA_POSTGRESQL"

  credentials {
    credential_pair {
      username = local.db_username
      password = local.db_password
    }
  }

  parameters {
    aurora_postgresql {
      database = local.db_name
      host     = data.aws_rds_cluster.this.reader_endpoint
      port     = data.aws_rds_cluster.this.port
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
