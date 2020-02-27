#!/bin/bash
# Purpose: Shell script to provision jenkins

set -x #Be verbose
set -e #Exit on first error

#
# Parse options
#

echo "Parse options..."
for i in "$@"
do
case $i in
  --ssh-username=*)
  SSH_USERNAME="${i#*=}"
  shift # past argument=value
  ;;
esac
done

#
# Use unallocated space to extend the home partition
#

# Create a new partition from unallocated space

# LSH BEGIN: changed for an "m5.xlarge" instance
# (
# echo n # Add a new partition
# echo p # Primary partition
# echo   # Partition number (Accept default)
# echo   # First sector (Accept default)
# echo   # Last sector (Accept default)
# echo w # Write changes
# ) | sudo fdisk /dev/xvda || true #This is here because fdisk always non-zero code
(
echo n # Add a new partition
echo p # Primary partition
echo   # Partition number (Accept default)
echo   # First sector (Accept default)
echo   # Last sector (Accept default)
echo w # Write changes
) | sudo fdisk /dev/nvme0n1 || true #This is here because fdisk always non-zero code
# LSH END: changed for an "m5.xlarge" instance

# Request that the operating system re-reads the partition table

sudo partprobe

# Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

# LSH BEGIN: changed for an "m5.xlarge" instance
# sudo pvcreate /dev/xvda3
sudo pvcreate /dev/nvme0n1p3
# LSH END: changed for an "m5.xlarge" instance

# Add the new physical volume to the volume group

# LSH BEGIN: changed for an "m5.xlarge" instance
# sudo vgextend VolGroup00 /dev/xvda3
sudo vgextend VolGroup00 /dev/nvme0n1p3
# LSH END: changed for an "m5.xlarge" instance

# Extend the size of the home logical volume

sudo lvextend -l +100%FREE /dev/mapper/VolGroup00-homeVol

# Expands the existing XFS filesystem

sudo xfs_growfs -d /dev/mapper/VolGroup00-homeVol

#
# Remove Nagios and Postfix
#

sudo yum -y remove nagios-common
sudo rpm -e postfix

#
# Install depedencies
#

sudo yum -y update

# LSH Testing environment BEGIN
sudo yum -y install epel-release
# LSH Testing environment END

sudo yum -y install \
  vim \
  tmux \
  yum-utils \
  device-mapper-persistent-data \
  lvm2 \
  python-pip \
  telnet \
  nc

# LSH Testing environment BEGIN
sudo yum -y install wget
sudo yum -y install mlocate
# LSH Testing environment END

sudo pip install awscli

# LSH BEGIN: Comment this out if running within sbdemo environment
# Disable trendmicro during builder
sudo service ds_agent stop
# LSH END: Comment this out if running within sbdemo environment

# Remove tty requirement for sudo in scripts and ssh calls
sudo sed -i.bak '/Defaults    requiretty/d' /etc/sudoers

# Prevent trendmicro from killing our docker network
sudo touch /etc/use_dsa_with_iptables
sudo chmod 755 /etc/use_dsa_with_iptables

# Docker
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo rpm --import https://download.docker.com/linux/centos/gpg
sudo yum-config-manager --enable rhel-7-server-extras-rpms
sudo yum-config-manager --enable rhui-REGION-rhel-server-extras
sudo yum -y install docker-ce-18.06.1.ce-3.el7

# LSH Testing environment BEGIN
# sudo usermod -aG docker ec2-user
# sudo usermod -aG docker centos
sudo usermod -aG docker $SSH_USERNAME
# LSH Testing environment END

sudo systemctl enable docker
sudo systemctl start docker

# Docker compose
sudo curl -L https://github.com/docker/compose/releases/download/1.18.0/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
sudo chmod 755 /usr/local/bin/docker-compose

# Change docker daemon configs
sudo mv /tmp/docker-daemon.json /etc/docker/daemon.json
sudo chmod 755 /etc/docker/daemon.json

#
# LSH BEGIN
#

# Install Jenkins dependencies
sudo yum install java-1.8.0-openjdk-devel -y
sudo yum install git -y

# Get the Jenkins yum repo
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat/jenkins.repo

# RPM import the Jenkins key
sudo rpm --import https://pkg.jenkins.io/redhat/jenkins.io.key

# Install Jenkins
sudo yum install jenkins -y

#
# LSH END
#
