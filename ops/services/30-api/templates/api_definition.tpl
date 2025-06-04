[{
  "name": "ab2d-api",
    "image": "${api_image}",
  "essential": true,
  "cpu": ${ecs_task_def_cpu_api},
  "memory": ${ecs_task_def_memory_api},
  "portMappings": [
    {
      "containerPort": ${container_port},
      "hostPort": ${alb_listener_port}
    }
  ],
  "mountPoints": [
    {
      "containerPath": "/mnt/efs",
      "sourceVolume": "efs"
    }
  ],
  "environment" : [
        {
      "name" : "AB2D_DB_HOST",
      "value" : "${db_host}"
    },
        {
      "name" : "AB2D_DB_PORT",
      "value" : "${db_port}"
    },
    {
      "name" : "AB2D_DB_USER",
      "value" : "${db_username}"
    },
    {
      "name" : "AB2D_DB_PASSWORD",
      "value" : "${db_password}"
    },
    {
      "name" : "AB2D_DB_DATABASE",
      "value" : "${db_name}"
    },
        {
      "name" : "AB2D_EFS_MOUNT",
      "value" : "/mnt/efs"
    },
    {
      "name" : "AB2D_EXECUTION_ENV",
      "value" : "${execution_env}"
    },
    {
       "name" : "AB2D_BFD_INSIGHTS",
       "value" : "${bfd_insights}"
    },
    {
      "name" : "AB2D_DB_SSL_MODE",
      "value" : "require"
    },
    {
      "name" : "AB2D_KEYSTORE_LOCATION",
      "value" : "${ab2d_keystore_location}"
    },
    {
      "name" : "AB2D_KEYSTORE_PASSWORD",
      "value" : "${ab2d_keystore_password}"
    },
    {
      "name" : "AB2D_OKTA_JWT_ISSUER",
      "value" : "${ab2d_okta_jwt_issuer}"
    },
    {
      "name": "AB2D_V2_ENABLED",
      "value": "${ab2d_v2_enabled}"
    },
    {
      "name" : "NEW_RELIC_APP_NAME",
      "value" : "${new_relic_app_name}"
    },
    {
      "name" : "NEW_RELIC_LICENSE_KEY",
      "value" : "${new_relic_license_key}"
    },
    {
      "name" : "AB2D_HPMS_URL",
      "value" : "${hpms_url}"
    },
    {
      "name" : "AB2D_HPMS_API_PARAMS",
      "value" : "${hpms_api_params}"
    },
    {
      "name" : "HPMS_AUTH_KEY_ID",
      "value" : "${hpms_auth_key_id}"
    },
    {
      "name" : "HPMS_AUTH_KEY_SECRET",
      "value" : "${hpms_auth_key_secret}"
    },
    {
      "name": "AB2D_SLACK_ALERT_WEBHOOKS",
      "value": "${slack_alert_webhooks}"
    },
    {
      "name": "AB2D_SLACK_TRACE_WEBHOOKS",
      "value": "${slack_trace_webhooks}"
    },
    {
      "name": "AWS_SQS_URL",
      "value": "${sqs_url}"
    },
    {
      "name": "AWS_SQS_FEATURE_FLAG",
      "value": "${sqs_feature_flag}"
    },
    {
      "name": "PROPERTIES_SERVICE_URL",
      "value": "${properties_service_url}"
    },
    {
      "name": "PROPERTIES_SERVICE_FEATURE_FLAG",
      "value": "${properties_service_feature_flag}"
    },
    {
      "name": "CONTRACTS_SERVICE_FEATURE_FLAG",
      "value": "${contracts_service_feature_flag}"
    }
  ],
  "logConfiguration": {
    "logDriver": "syslog"
  },
  "healthCheck": null
}]
