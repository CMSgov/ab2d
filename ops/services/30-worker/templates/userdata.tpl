#!/bin/bash

#
# Set more useful hostname
#

echo "$(hostname -s).${env}" > /tmp/hostname
sudo mv /tmp/hostname /etc/hostname
sudo hostname "$(hostname -s).${env}"

AWS_REGION="${aws_region}"
export AWS_REGION

## Add an extra hop for docker networking with IMDSv2
TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
echo $TOKEN
INSTANCE_ID=$(curl -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-id)
echo $INSTANCE_ID
aws ec2 modify-instance-metadata-options --instance-id $INSTANCE_ID --http-put-response-hop-limit 2

#####
# --------
# With TLS
# --------
# Mount with IAM authorization to an Amazon EC2 instance that has an instance profile
echo '${efs_id}:/ /mnt/efs efs _netdev,tls,iam,accesspoint=${accesspoint} 0 0' | sudo tee -a /etc/fstab
sudo mount -a
#####

#
# Download keystore from S3 to the EFS mount
#

# Create a "bfd-keystore" directory under EFS if it doesn't exist
keystore_dir="/mnt/efs/bfd-keystore/${env}"
echo -n "Creating keystore directory at $${keystore_dir}..."
mkdir -p "$${keystore_dir}"
echo "done."

# Download keystore file from S3
aws s3 cp "s3://${bucket_name}/${bfd_keystore_file_name}" "$${keystore_dir}/${bfd_keystore_file_name}"

# ECS config file
# https://github.com/aws/amazon-ecs-agent
sudo mkdir -p /etc/ecs
sudo sh -c 'echo "
ECS_DATADIR=/data
ECS_ENABLE_TASK_IAM_ROLE=true
ECS_ENABLE_TASK_IAM_ROLE_NETWORK_HOST=true
ECS_LOGFILE=/log/ecs-agent.log
ECS_AVAILABLE_LOGGING_DRIVERS=[\"json-file\",\"awslogs\",\"syslog\",\"none\"]
ECS_ENABLE_TASK_IAM_ROLE=true
ECS_UPDATE_DOWNLOAD_DIR=/var/cache/ecs
SSL_CERT_DIR=/etc/pki/tls/certs
ECS_ENABLE_AWSLOGS_EXECUTIONROLE_OVERRIDE=true
ECS_UPDATES_ENABLED=true
ECS_ENABLE_TASK_IAM_ROLE_NETWORK_HOST=true
ECS_LOGFILE=/log/ecs-agent.log
ECS_CLUSTER="${cluster_name}"
ECS_LOGLEVEL=info" > /etc/ecs/ecs.config'

# Autostart the ecs client
sudo docker run --name ecs-agent \
  --detach=true \
  --restart=on-failure:10 \
  --volume=/var/run:/var/run \
  --volume=/var/log/ecs/:/log \
  --volume=/var/lib/ecs/data:/data \
  --volume=/etc/ecs:/etc/ecs \
  --net=host \
  --env-file=/etc/ecs/ecs.config \
  --privileged \
  --env-file=/etc/ecs/ecs.config \
  amazon/amazon-ecs-agent:latest
