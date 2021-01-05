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

curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# LSH BEGIN: Comment this out if running within sbdemo environment
# Disable trendmicro during builder
sudo service ds_agent stop
# LSH END: Comment this out if running within sbdemo environment

# Remove tty requirement for sudo in scripts and ssh calls

sudo sed -i.bak '/Defaults    requiretty/d' /etc/sudoers

# Prevent trendmicro from killing our docker network

sudo touch /etc/use_dsa_with_iptables
sudo chmod 755 /etc/use_dsa_with_iptables

# Configure log rotation for '/var/log/messages'

sudo sed -i.bak '/messages/ r /deployment/logrotate-var-log-messages-config-snippet' /etc/logrotate.d/syslog

# Install Docker

sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo rpm --import https://download.docker.com/linux/centos/gpg
sudo yum-config-manager --enable 'rhel-7-server-extras-rpms'
sudo yum-config-manager --enable 'rhui-REGION-rhel-server-extras'
sudo rpm --import https://www.centos.org/keys/RPM-GPG-KEY-CentOS-7
sudo yum install -y http://mirror.centos.org/centos/7/extras/x86_64/Packages/container-selinux-2.107-3.el7.noarch.rpm

# TO DO: Update this when latest gold disk resolves the issue.
# Temporary workaround for an error caused by the following URL change
# - before: https://download.docker.com/linux/centos/7Server/
# - after: https://download.docker.com/linux/centos/7/
sudo sed -i 's%\$releasever%7%g' /etc/yum.repos.d/docker-ce.repo

sudo yum -y install docker-ce-19.03.8-3.el7
sudo usermod -aG docker $SSH_USERNAME
sudo systemctl enable docker
sudo systemctl start docker

# Docker compose

sudo curl -L https://github.com/docker/compose/releases/download/1.26.2/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
sudo chmod 755 /usr/local/bin/docker-compose

# Change docker daemon configs

sudo mv /tmp/docker-daemon.json /etc/docker/daemon.json
sudo chmod 755 /etc/docker/daemon.json

# Install Jenkins dependencies

sudo yum install java-1.8.0-openjdk-devel -y
sudo yum install git -y

# Get the Jenkins yum repo

sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat/jenkins.repo

# RPM import the Jenkins key

sudo rpm --import https://pkg.jenkins.io/redhat/jenkins.io.key

# Install Jenkins

# sudo yum install jenkins -y
