[
  {
    "name": "worker",
    "image": "${worker_image}",
    "essential": true,
    "cpu": ${ecs_task_def_cpu_worker},
    "memory": ${ecs_task_def_memory_worker},
    "mountPoints": [
      {
        "containerPath": "/mnt/efs",
        "sourceVolume": "efs"
      }
    ],
    "environment": [
      {
        "name": "AB2D_BFD_KEYSTORE_LOCATION",
        "value": "${bfd_keystore_location}"
      },
      {
        "name": "AB2D_BFD_KEYSTORE_PASSWORD",
        "value": "${bfd_keystore_password}"
      },
      {
        "name": "AB2D_BFD_URL",
        "value": "${bfd_url}"
      },
      {
        "name": "AB2D_DB_DATABASE",
        "value": "${db_name}"
      },
      {
        "name": "AB2D_DB_HOST",
        "value": "${db_host}"
      },
      {
        "name": "AB2D_DB_PASSWORD",
        "value": "${db_password}"
      },
      {
        "name": "AB2D_DB_PORT",
        "value": "${db_port}"
      },
      {
        "name": "AB2D_DB_USER",
        "value": "${db_username}"
      },
      {
        "name": "AB2D_EFS_MOUNT",
        "value": "/mnt/efs"
      },
      {
        "name": "AB2D_EXECUTION_ENV",
        "value": "${execution_env}"
      },
      {
        "name": "AB2D_BFD_INSIGHTS",
        "value": "${bfd_insights}"
      },
      {
        "name": "AB2D_DB_SSL_MODE",
        "value": "require"
      },
      {
        "name": "AB2D_JOB_POOL_CORE_SIZE",
        "value": "${max_concurrent_eob_jobs}"
      },
      {
        "name": "AB2D_JOB_POOL_MAX_SIZE",
        "value": "${max_concurrent_eob_jobs}"
      },
      {
        "name": "IMAGE_VERSION",
        "value": "${image_version}"
      },
      {
        "name": "NEW_RELIC_APP_NAME",
        "value": "${new_relic_app_name}"
      },
      {
        "name": "NEW_RELIC_LICENSE_KEY",
        "value": "${new_relic_license_key}"
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
  }
]
