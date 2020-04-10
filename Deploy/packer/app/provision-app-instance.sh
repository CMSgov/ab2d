#!/bin/bash
# Purpose: Shell script to provision image for app instances

set -x # Be verbose
set -e # Exit on first error
export APP_DIR=$HOME/app/

#
# Parse options
#

echo "Parse options..."
for i in "$@"
do
  case $i in
  --environment=*)
  ENVIRONMENT="${i#*=}"
  shift # past argument=value
  ;;
  --region=*)
  REGION="${i#*=}"
  shift # past argument=value
  ;;
  --ssh-username=*)
  SSH_USERNAME="${i#*=}"
  shift # past argument=value
  ;;
esac
done

#
# Update yum
#

sudo yum -y update

#
# Use unallocated space to extend the var and home partitions
#

# Install logical volume manager

sudo yum install -y lvm2

# # Create a new partition from unallocated space

# # LSH BEGIN: changed for an "m5.xlarge" instance
# # (
# # echo n # Add a new partition
# # echo p # Primary partition
# # echo   # Partition number (Accept default)
# # echo   # First sector (Accept default)
# # echo   # Last sector (Accept default)
# # echo w # Write changes
# # ) | sudo fdisk /dev/xvda || true #This is here because fdisk always non-zero code
# (
# echo n # Add a new partition
# echo p # Primary partition
# echo   # Partition number (Accept default)
# echo   # First sector (Accept default)
# echo   # Last sector (Accept default)
# echo w # Write changes
# ) | sudo fdisk /dev/nvme0n1 || true #This is here because fdisk always non-zero code
# # LSH END: changed for an "m5.xlarge" instance

# # Request that the operating system re-reads the partition table

# sudo partprobe

# # Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

# # LSH BEGIN: changed for an "m5.xlarge" instance
# # sudo pvcreate /dev/xvda3
# sudo pvcreate /dev/nvme0n1p3
# # LSH END: changed for an "m5.xlarge" instance

# # Add the new physical volume to the volume group

# # LSH BEGIN: changed for an "m5.xlarge" instance
# # sudo vgextend VolGroup00 /dev/xvda3
# sudo vgextend VolGroup00 /dev/nvme0n1p3
# # LSH END: changed for an "m5.xlarge" instance

# # Extend the size of the var logical volume

# sudo lvextend -l +50%FREE /dev/mapper/VolGroup00-varVol

# # Extend the size of the home logical volume

# sudo lvextend -l +50%FREE /dev/mapper/VolGroup00-homeVol

# # Expand the existing XFS filesystem for the var logical volume

# sudo xfs_growfs -d /dev/mapper/VolGroup00-varVol

# # Expand the existing XFS filesystem for the home logical volume

# sudo xfs_growfs -d /dev/mapper/VolGroup00-homeVol

# Remove Nagios and Postfix
sudo yum -y remove nagios-common
sudo rpm -e postfix

# Install depedencies
sudo yum -y install \
  vim \
  tmux \
  yum-utils \
  python-pip \
  telnet \
  nc \
  wget \
  epel-release \
  python-pip

# Postgres 11

wget https://download.postgresql.org/pub/repos/yum/RPM-GPG-KEY-PGDG-11
sudo rpm --import RPM-GPG-KEY-PGDG-11
sudo yum -y install https://download.postgresql.org/pub/repos/yum/11/redhat/rhel-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm

sudo yum -y install postgresql11

# Docker

sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo rpm --import https://download.docker.com/linux/centos/gpg
sudo yum-config-manager --enable 'rhel-7-server-extras-rpms'
sudo yum-config-manager --enable 'rhui-REGION-rhel-server-extras'
sudo rpm --import https://www.centos.org/keys/RPM-GPG-KEY-CentOS-7
sudo yum install -y http://mirror.centos.org/centos/7/extras/x86_64/Packages/container-selinux-2.107-3.el7.noarch.rpm
sudo yum -y install docker-ce-19.03.8-3.el7
sudo usermod -aG docker $SSH_USERNAME
sudo systemctl enable docker
sudo systemctl start docker

# Install awscli

sudo pip install awscli

# Disable trendmicro, and Amazon SSM
# sudo systemctl disable amazon-ssm-agent
# sudo systemctl disable ds_agent
# sudo systemctl disable ds_filter

# Remove tty requirement for sudo in scripts and ssh calls

sudo sed -i.bak '/Defaults    requiretty/d' /etc/sudoers

# Prevent trendmicro from killing our docker network

sudo touch /etc/use_dsa_with_iptables
sudo chmod 755 /etc/use_dsa_with_iptables

# Increase default rsyslog rate limit threshold

sudo sed -i '/SystemLogRateLimitBurst/c\$SystemLogRateLimitBurst 2000' /etc/rsyslog.conf

# Configure New Relic infrastructure agent

cd /tmp
aws s3 cp "s3://${ENVIRONMENT}-automation/encrypted-files/newrelic-infra.yml.encrypted" ./newrelic-infra.yml.encrypted
aws kms --region "${REGION}" decrypt \
  --ciphertext-blob fileb://newrelic-infra.yml.encrypted \
  --output text \
  --query Plaintext \
  | base64 --decode \
  > newrelic-infra.yml
[ -s newrelic-infra.yml ] || (echo "NewRelic file decryption failed" && exit 1)
sudo mv newrelic-infra.yml /etc/newrelic-infra.yml

# Restart New Relic infrastructure agent

sudo systemctl restart newrelic-infra

#
# Setup Ruby environment
#

# Install rbenv dependencies

sudo yum install -y git-core zlib zlib-devel gcc-c++ patch readline \
  readline-devel libyaml-devel libffi-devel openssl-devel make bzip2 \
  autoconf automake libtool bison curl sqlite-devel

# Install rbenv and ruby-build

set +e # Ignore errors
curl -sL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-installer | bash -
set -e # Exit on next error
echo "*********************************************************"
echo "NOTE: Ignore the 'not found' message in the above output."
echo "*********************************************************"

# Add rbenv initialization to "bashrc"

echo 'export PATH="$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
echo 'eval "$(rbenv init -)"' >> ~/.bashrc

# Initialize rbenv for the current session

export PATH="$HOME/.rbenv/bin:$PATH"
eval "$(rbenv init -)"

# Install Ruby 2.6.5

echo "NOTE: the ruby install takes a while..."
rbenv install 2.6.5

# Set the global version of Ruby

rbenv global 2.6.5

# Install bundler

gem install bundler

# Update Ruby Gems

gem update --system

# Change to the "/tmp" directory

cd /tmp

# Ensure required gems are installed

bundle install

#
# Make sure ec2 userdata is enabled
#

sudo touch /var/tmp/initial

# ECS agent
# https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-agent-install.html#ecs-agent-install-nonamazonlinux

sudo sh -c "echo 'net.ipv4.conf.all.route_localnet = 1' >> /etc/sysctl.conf"
sudo sysctl -p /etc/sysctl.conf
sudo iptables -t nat -A PREROUTING -p tcp -d 169.254.170.2 --dport 80 -j DNAT --to-destination 127.0.0.1:51679
sudo iptables -t nat -A OUTPUT -d 169.254.170.2 -p tcp -m tcp --dport 80 -j REDIRECT --to-ports 51679
sudo mkdir -p /var/log/ecs /var/lib/ecs/data
sudo sh -c 'echo "export NO_PROXY=169.254.169.254" >> /etc/sysconfig/docker'
sudo sh -c 'iptables-save > /etc/sysconfig/iptables'
sudo docker pull amazon/amazon-ecs-agent:latest
