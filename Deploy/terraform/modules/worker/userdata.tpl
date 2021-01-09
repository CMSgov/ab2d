#!/bin/bash

#
# Set more useful hostname
#

echo "$(hostname -s).${env}" > /tmp/hostname
sudo mv /tmp/hostname /etc/hostname
sudo hostname "$(hostname -s).${env}"

#####
# -----------
# Without TLS
# -----------
# echo '${efs_id}:/ /mnt/efs efs _netdev 0 0' | sudo tee -a /etc/fstab
# sudo mount -a
#
# --------
# With TLS
# --------
# Mount with IAM authorization to an Amazon EC2 instance that has an instance profile
echo '${efs_id}:/ /mnt/efs efs _netdev,tls,iam 0 0' | sudo tee -a /etc/fstab
sudo mount -a
#####

# Place BFD keystore in shared EFS directory (if doesn't already exist)

if [[ -d "/mnt/efs/bfd-keystore/${env}" ]] && [[ -f "/mnt/efs/bfd-keystore/${env}/${bfd_keystore_file_name}" ]]; then

  echo "NOTE: BFD keystore already exists in EFS."

else

  #
  # Get keystore from S3, decrypt it, and move it to EFS
  #

  # Change to the "/deployment" directory
  cd /deployment

  # Commented out because packer installs ruby under ec2_user, while user data runs as root
  #
  # Get keystore from S3 and decrypt it
  # bundle exec rake get_file_from_s3_and_decrypt["./${bfd_keystore_file_name}","${env}-automation"]
  #
  # Get keystore from S3 and decrypt it
  export RUBY_BIN="/home/ec2-user/.rbenv/versions/2.6.5/bin"
  sudo "$RUBY_BIN/bundle" exec "$RUBY_BIN/rake" \
    get_file_from_s3_and_decrypt["./${bfd_keystore_file_name}","${env}-automation"]

  # Create a "bfd-keystore" directory under EFS (if doesn't exist)
  sudo mkdir -p "/mnt/efs/bfd-keystore/${env}"

  # Move the BFD keystore to the "bfd-keystore" directory
  sudo mv "/tmp/${bfd_keystore_file_name}" "/mnt/efs/bfd-keystore/${env}"

fi

#
# Setup ECS realted items
#

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
