#!/bin/bash
# Purpose: Shell script to provision image for app instances

set -x #Be verbose
set -e #Exit on first error
export APP_DIR=$HOME/app/

# Remove Nagios and Postfix
sudo yum -y remove nagios-common
sudo rpm -e postfix

# Install depedencies
sudo yum -y update
sudo yum -y install \
  vim \
  tmux \
  yum-utils \
  python-pip \
  telnet

# Postgres 10
wget https://download.postgresql.org/pub/repos/yum/RPM-GPG-KEY-PGDG-10
sudo rpm --import RPM-GPG-KEY-PGDG-10
sudo yum -y install https://download.postgresql.org/pub/repos/yum/10/redhat/rhel-7-x86_64/pgdg-redhat10-10-2.noarch.rpm
sudo yum -y install postgresql10

# Docker
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo rpm --import https://download.docker.com/linux/centos/gpg
sudo yum-config-manager --enable rhel-7-server-extras-rpms
sudo yum-config-manager --enable rhui-REGION-rhel-server-extras
sudo yum -y install docker-ce-18.06.1.ce-3.el7
sudo usermod -aG docker ec2-user
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

#
# LSH Comment out gold disk related section
#
# # Update splunk forwarder config
# sudo chown splunk:splunk /tmp/splunk-deploymentclient.conf
# sudo mv -f /tmp/splunk-deploymentclient.conf /opt/splunkforwarder/etc/system/local/deploymentclient.conf

#
# LSH *** TO DO ***: Need to implement S3 with "encrypted-newrelic-infra.yml" file
#
# # Install newrelic infrastructure agent
# cd /tmp
# aws s3 cp s3://cms-ab2d-automation/encrypted-newrelic-infra.yml ./encrypted-newrelic-infra.yml
# aws kms --region us-east-1 decrypt --ciphertext-blob fileb://encrypted-newrelic-infra.yml --output text --query Plaintext | base64 --decode > newrelic-infra.yml
# [ -s newrelic-infra.yml ] || (echo "NewRelic file decryption failed" && exit 1)
# sudo mv newrelic-infra.yml /etc/newrelic-infra.yml
# sudo curl -o /etc/yum.repos.d/newrelic-infra.repo https://download.newrelic.com/infrastructure_agent/linux/yum/el/7/x86_64/newrelic-infra.repo
# sudo yum -q makecache -y --disablerepo='*' --enablerepo='newrelic-infra'
# sudo yum install newrelic-infra -y

#
# LSH Comment out gold disk related section
#
# # Make sure splunk can read all logs
# sudo /opt/splunkforwarder/bin/splunk stop
# sudo /opt/splunkforwarder/bin/splunk clone-prep-clear-config
# sudo /opt/splunkforwarder/bin/splunk start

# Make sure ec2 userdata is enabled
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
