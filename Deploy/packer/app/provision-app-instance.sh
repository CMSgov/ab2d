#!/bin/bash -l
# Note: that "-l" was added to the first line to make bash a login shell
# - this causes .bash_profile and .bashrc to be sourced which is needed for ruby items

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
  telnet \
  nc \
  wget \
  epel-release \
  python-pip

# Install Postgres 11

wget https://download.postgresql.org/pub/repos/yum/RPM-GPG-KEY-PGDG-11
sudo rpm --import RPM-GPG-KEY-PGDG-11
sudo yum -y install https://download.postgresql.org/pub/repos/yum/11/redhat/rhel-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm

sudo yum -y install postgresql11

# Install Postgres 10 devel
# - note that Postgres 11 devel could not be used because it had dependencies that require RedHat subscription changes that are not available
# - need devel in order to use a newer version of pg_dump

sudo yum -y install postgresql10-devel

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

# Configure log rotation for '/var/log/messages'

sudo sed -i.bak '/messages/ r /deployment/logrotate-var-log-messages-config-snippet' /etc/logrotate.d/syslog

# Configure New Relic infrastructure agent

cd /tmp

if [ "${ENVIRONMENT}" == "ab2d-east-prod-test" ]; then
  S3_BUCKET=ab2d-east-prod-test-main
else
  S3_BUCKET="${ENVIRONMENT}-automation"
fi

aws s3 cp "s3://${S3_BUCKET}/encrypted-files/newrelic-infra.yml.encrypted" ./newrelic-infra.yml.encrypted
aws kms --region "${REGION}" decrypt \
  --ciphertext-blob fileb://newrelic-infra.yml.encrypted \
  --output text \
  --query Plaintext \
  | base64 --decode \
  > newrelic-infra.yml
[ -s newrelic-infra.yml ] || (echo "NewRelic file decryption failed" && exit 1)
sudo mv newrelic-infra.yml /etc/newrelic-infra.yml

# Enable New Relic infrastructure agent

sudo systemctl enable newrelic-infra

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

# Change to the "/deployment" directory

cd /deployment

# Add the pgsql-10 binary directory to the path (required to install the pg gem)

export PATH=$PATH:/usr/pgsql-10/bin

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
