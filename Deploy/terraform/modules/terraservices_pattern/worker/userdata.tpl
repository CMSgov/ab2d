#!/bin/bash

#
# Set more useful hostname
#

echo "$(hostname -s).${env}" > /tmp/hostname
sudo mv /tmp/hostname /etc/hostname
sudo hostname "$(hostname -s).${env}"

#
# Setup EFS realted items 
#
 
# Build amazon-efs-utils as an RPM package

sudo yum -y install git
sudo yum -y install rpm-build
cd /tmp
git clone https://github.com/aws/efs-utils
cd efs-utils
sudo make rpm

# Install amazon-efs-utils as an RPM package
# - note that '--nogpgcheck' is now required for installing locally built rpm

sudo yum -y install ./build/amazon-efs-utils*rpm --nogpgcheck

#
# Upgrade stunnel for using EFS mount helper with TLS
# - by default, it enforces certificate hostname checking
#

sudo yum install gcc openssl-devel tcp_wrappers-devel -y
cd /tmp
curl -o "${stunnel_latest_version}.tar.gz" "https://www.stunnel.org/downloads/${stunnel_latest_version}.tar.gz"
tar xvfz "${stunnel_latest_version}.tar.gz"
cd "${stunnel_latest_version}"
sudo ./configure
sudo make
sudo rm -f /bin/stunnel
sudo make install
if [[ -f /bin/stunnel ]]; then sudo mv /bin/stunnel /root; fi
sudo ln -s /usr/local/bin/stunnel /bin/stunnel

# Configure running container instances to use an Amazon EFS file system
#
# Mounting Your Amazon EFS File System Automatically
# https://docs.aws.amazon.com/efs/latest/ug/mount-fs-auto-mount-onreboot.html

sudo mkdir /mnt/efs
sudo cp /etc/fstab /etc/fstab.bak

# TO DO: This will be handled differently when we move to fargate
#####
# -----------
# TO DO: Ensure stunnel is being used with the custom AMI
# -----------
echo '${efs_id}:/ /mnt/efs efs _netdev 0 0' | sudo tee -a /etc/fstab
sudo mount -a
#
# --------
# Note that the following method can't be used since it is specific to Amazon's ECS specific AMI)
# --------
# Mount with IAM authorization to an Amazon EC2 instance that has an instance profile
# echo '${efs_id}:/ /mnt/efs efs _netdev,tls,iam 0 0' | sudo tee -a /etc/fstab
# sudo mount -a
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

  # Note that packer installs ruby under ec2_user, while user data runs as root
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
