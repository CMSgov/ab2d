#!/bin/bash

# Set more useful hostname
echo "$(hostname -s).ab2d-${env}" > /tmp/hostname
sudo mv /tmp/hostname /etc/hostname
sudo hostname "$(hostname -s).ab2d-${env}"

#
# Setup EFS realted items 
#
 
# Build and install amazon-efs-utils as an RPM package
sudo yum -y install git
sudo yum -y install rpm-build
cd /tmp
git clone https://github.com/aws/efs-utils
cd efs-utils
sudo make rpm
sudo yum -y install ./build/amazon-efs-utils*rpm

# Upgrade stunnel for using EFS mount helper with TLS
# - by default, it enforces certificate hostname checking
sudo yum install gcc openssl-devel tcp_wrappers-devel -y
cd /tmp
curl -o stunnel-5.55.tar.gz https://www.stunnel.org/downloads/stunnel-5.55.tar.gz
tar xvfz stunnel-5.55.tar.gz
cd stunnel-5.55
sudo ./configure
sudo make
sudo rm -f /bin/stunnel
sudo make install
if [[ -f /bin/stunnel ]]; then sudo mv /bin/stunnel /root; fi
sudo ln -s /usr/local/bin/stunnel /bin/stunnel

# Configure running container instances to use an Amazon EFS file system
sudo mkdir /mnt/efs
sudo cp /etc/fstab /etc/fstab.bak
echo 'fs-32aa74b3 /mnt/efs efs _netdev,tls 0 0' | sudo tee -a /etc/fstab
sudo mount -a

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
