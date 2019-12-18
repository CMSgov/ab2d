resource "aws_wafregional_sql_injection_match_set" "sql_injection_match_set" {
  name = "ab2d-${var.env}-sql-injection-match-set"

  sql_injection_match_tuple {
    text_transformation = "HTML_ENTITY_DECODE"

    field_to_match {
      type = "BODY"
    }
  }

  sql_injection_match_tuple {
    text_transformation = "URL_DECODE"

    field_to_match {
      type = "BODY"
    }
  }

  sql_injection_match_tuple {
    text_transformation = "HTML_ENTITY_DECODE"

    field_to_match {
      type = "URI"
    }
  }

  sql_injection_match_tuple {
    text_transformation = "URL_DECODE"

    field_to_match {
      type = "URI"
    }
  }

  sql_injection_match_tuple {
    text_transformation = "HTML_ENTITY_DECODE"

    field_to_match {
      type = "QUERY_STRING"
    }
  }

  sql_injection_match_tuple {
    text_transformation = "URL_DECODE"

    field_to_match {
      type = "QUERY_STRING"
    }
  }

  sql_injection_match_tuple {
    text_transformation = "HTML_ENTITY_DECODE"

    field_to_match {
      type = "HEADER"
      data = "cookie"
    }
  }

  sql_injection_match_tuple {
    text_transformation = "URL_DECODE"

    field_to_match {
      type = "HEADER"
      data = "cookie"
    }
  }
}

resource "aws_wafregional_rule" "sql_injection" {
  name        = "ab2d-${lower(var.env)}-sqlinjection"

  # metric_name can contain only 1 to 128 alphanumeric characters (A-Z, a-z, 0-9)
  metric_name = "ab2d${replace(var.env, "-", "")}sqlinjection"

  predicate {
    data_id = "${aws_wafregional_sql_injection_match_set.sql_injection_match_set.id}"
    negated = false
    type    = "SqlInjectionMatch"
  }
}

resource "aws_wafregional_xss_match_set" "xss_match_set" {
  name = "ab2d-${lower(var.env)}-xss-match-set"

  xss_match_tuple {
    text_transformation = "HTML_ENTITY_DECODE"

    field_to_match {
      type = "BODY"
    }
  }

  xss_match_tuple {
    text_transformation = "URL_DECODE"

    field_to_match {
      type = "BODY"
    }
  }

  xss_match_tuple {
    text_transformation = "HTML_ENTITY_DECODE"

    field_to_match {
      type = "URI"
    }
  }

  xss_match_tuple {
    text_transformation = "URL_DECODE"

    field_to_match {
      type = "URI"
    }
  }

  xss_match_tuple {
    text_transformation = "HTML_ENTITY_DECODE"

    field_to_match {
      type = "QUERY_STRING"
    }
  }

  xss_match_tuple {
    text_transformation = "URL_DECODE"

    field_to_match {
      type = "QUERY_STRING"
    }
  }

  xss_match_tuple {
    text_transformation = "HTML_ENTITY_DECODE"

    field_to_match {
      type = "HEADER"
      data = "cookie"
    }
  }

  xss_match_tuple {
    text_transformation = "URL_DECODE"

    field_to_match {
      type = "HEADER"
      data = "cookie"
    }
  }
}

resource "aws_wafregional_rule" "xss" {
  name        = "ab2d-${lower(var.env)}-xss"

  # metric_name can contain only 1 to 128 alphanumeric characters (A-Z, a-z, 0-9)
  metric_name = "ab2d${replace(var.env, "-", "")}xss"

  predicate {
    data_id = "${aws_wafregional_xss_match_set.xss_match_set.id}"
    negated = false
    type    = "XssMatch"
  }
}

resource "aws_wafregional_web_acl" "web_acl" {
  name        = "ab2d-${lower(var.env)}-web-acl"

  # metric_name can contain only 1 to 128 alphanumeric characters (A-Z, a-z, 0-9)
  metric_name = "ab2d${replace(var.env, "-", "")}webacl"

  default_action {
    type = "ALLOW"
  }

  rule {
    action {
      type = "BLOCK"
    }

    priority = 2
    rule_id  = "${aws_wafregional_rule.sql_injection.id}"
    type     = "REGULAR"
  }
  rule {
    action {
      type = "BLOCK"
    }

    priority = 3
    rule_id  = "${aws_wafregional_rule.xss.id}"
    type     = "REGULAR"
  }

}

resource "aws_wafregional_web_acl_association" "acl_association" {
  resource_arn = "${var.alb_arn}"
  web_acl_id = "${aws_wafregional_web_acl.web_acl.id}"
}