resource "aws_wafregional_sql_injection_match_set" "sql_injection_match_set" {
  name = "${var.env}-sql-injection-match-set"

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
  name        = "${lower(var.env)}-sqlinjection"

  # metric_name can contain only 1 to 128 alphanumeric characters (A-Z, a-z, 0-9)
  metric_name = "${replace(var.env, "-", "")}sqlinjection"

  predicate {
    data_id = "${aws_wafregional_sql_injection_match_set.sql_injection_match_set.id}"
    negated = false
    type    = "SqlInjectionMatch"
  }
}

resource "aws_wafregional_xss_match_set" "xss_match_set" {
  name = "${lower(var.env)}-xss-match-set"

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
  name        = "${lower(var.env)}-xss"

  # metric_name can contain only 1 to 128 alphanumeric characters (A-Z, a-z, 0-9)
  metric_name = "${replace(var.env, "-", "")}xss"

  predicate {
    data_id = "${aws_wafregional_xss_match_set.xss_match_set.id}"
    negated = false
    type    = "XssMatch"
  }
}

resource "aws_wafregional_ipset" "ipset" {
  name = "${lower(var.env)}-ip-set"

  ip_set_descriptor {
    type  = "IPV4"
    value = "0.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "1.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "2.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "3.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "4.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "5.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "6.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "7.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "8.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "9.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "10.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "11.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "12.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "13.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "14.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "15.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "16.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "17.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "18.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "19.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "20.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "21.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "22.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "23.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "24.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "25.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "26.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "27.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "28.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "29.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "30.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "31.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "32.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "33.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "34.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "35.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "36.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "37.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "38.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "39.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "40.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "41.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "42.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "43.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "44.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "45.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "46.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "47.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "48.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "49.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "50.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "51.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "52.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "53.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "54.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "55.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "56.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "57.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "58.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "59.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "60.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "61.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "62.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "63.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "64.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "65.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "66.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "67.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "68.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "69.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "70.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "71.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "72.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "73.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "74.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "75.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "76.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "77.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "78.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "79.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "80.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "81.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "82.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "83.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "84.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "85.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "86.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "87.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "88.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "89.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "90.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "91.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "92.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "93.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "94.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "95.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "96.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "97.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "98.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "99.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "100.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "101.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "102.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "103.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "104.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "105.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "106.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "107.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "108.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "109.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "110.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "111.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "112.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "113.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "114.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "115.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "116.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "117.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "118.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "119.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "120.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "121.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "122.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "123.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "124.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "125.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "126.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "127.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "128.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "129.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "130.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "131.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "132.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "133.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "134.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "135.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "136.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "137.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "138.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "139.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "140.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "141.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "142.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "143.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "144.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "145.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "146.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "147.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "148.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "149.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "150.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "151.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "152.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "153.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "154.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "155.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "156.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "157.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "158.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "159.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "160.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "161.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "162.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "163.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "164.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "165.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "166.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "167.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "168.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "169.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "170.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "171.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "172.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "173.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "174.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "175.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "176.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "177.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "178.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "179.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "180.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "181.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "182.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "183.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "184.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "185.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "186.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "187.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "188.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "189.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "190.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "191.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "192.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "193.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "194.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "195.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "196.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "197.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "198.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "199.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "200.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "201.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "202.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "203.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "204.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "205.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "206.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "207.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "208.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "209.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "210.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "211.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "212.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "213.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "214.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "215.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "216.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "217.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "218.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "219.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "220.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "221.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "222.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "223.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "224.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "225.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "226.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "227.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "228.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "229.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "230.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "231.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "232.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "233.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "234.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "235.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "236.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "237.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "238.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "239.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "240.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "241.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "242.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "243.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "244.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "245.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "246.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "247.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "248.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "249.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "250.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "251.0.0.0/8"
  } 

  ip_set_descriptor {
    type  = "IPV4"
    value = "252.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "253.0.0.0/8"
  }

  ip_set_descriptor {
    type  = "IPV4"
    value = "254.0.0.0/8"
  }
  
  ip_set_descriptor {
    type  = "IPV4"
    value = "255.0.0.0/8"
  }

}

resource "aws_wafregional_rate_based_rule" "rate-limit" {
  name        = "${lower(var.env)}-rate-limit"

  # metric_name can contain only 1 to 128 alphanumeric characters (A-Z, a-z, 0-9)
  metric_name = "${replace(var.env, "-", "")}ratelimit"
  
  rate_key   = "IP"
  rate_limit = 100

  predicate {
    data_id = "${aws_wafregional_ipset.ipset.id}"
    negated = false
    type    = "IPMatch"
  }
}

resource "aws_wafregional_web_acl" "web_acl" {
  name        = "${lower(var.env)}-web-acl"

  # metric_name can contain only 1 to 128 alphanumeric characters (A-Z, a-z, 0-9)
  metric_name = "${replace(var.env, "-", "")}webacl"

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