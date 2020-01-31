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

# Prepare for Amazon EFS file system mounting that occurs when user data scripts are run
sudo mkdir /mnt/efs
sudo cp /etc/fstab /etc/fstab.bak

# Configure running container instances to use an Amazon EFS file system
#####
# *** TO DO ***: resolve TLS issue
# echo '${efs_id} /mnt/efs efs _netdev,tls 0 0' | sudo tee -a /etc/fstab
# sudo mount -a
echo '${efs_id}:/ /mnt/efs efs _netdev 0 0' | sudo tee -a /etc/fstab
sudo mount -a
#####

##############################################################
# Moved "provision-app-instance.sh" under packer BEGIN
##############################################################

# #
# # Setup Ruby environment
# #

# # Install rbenv dependencies
# sudo yum install -y git-core zlib zlib-devel gcc-c++ patch readline \
#   readline-devel libyaml-devel libffi-devel openssl-devel make bzip2 \
#   autoconf automake libtool bison curl sqlite-devel

# # Install rbenv and ruby-build
# curl -sL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-installer | bash -
# echo "*********************************************************"
# echo "NOTE: Ignore the 'not found' message in the above output."
# echo "*********************************************************"

# # Add rbenv initialization to "bashrc"
# echo 'export PATH="$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
# echo 'eval "$(rbenv init -)"' >> ~/.bashrc

# # Initialize rbenv for the current session
# export PATH="$HOME/.rbenv/bin:$PATH"
# eval "$(rbenv init -)"

# # Install Ruby 2.6.5
# echo "NOTE: the ruby install takes a while..."
# rbenv install 2.6.5

# # Set the global version of Ruby
# rbenv global 2.6.5

# # Install bundler
# gem install bundler

# # Update Ruby Gems
# gem update --system

##############################################################
# Moved "provision-app-instance.sh" under packer END
##############################################################


# Place BFD keystore in shared EFS directory (if doesn't already exist)
if [[ -d "/mnt/efs/bfd-keystore/${env}" ]] && [[ -f "/mnt/efs/bfd-keystore/${env}/ab2d_sbx_keystore" ]]; then

  echo "NOTE: BFD keystore already exists in EFS."

else

  ##############################################################
  # Moved "provision-app-instance.sh" under packer END
  ##############################################################

  # # Change to the "/tmp" directory
  # cd /tmp

  # # Ensure required gems are installed
  # bundle install

  ##############################################################
  # Moved "provision-app-instance.sh" under packer END
  ##############################################################

  #
  # Get keystore from S3, decrypt it, and move it to EFS
  #

  # Change to the "/tmp" directory
  cd /tmp

  # Commented out because packer installs ruby under ec2_user, while user data runs as root
  #
  # Get keystore from S3 and decrypt it
  # bundle exec rake get_file_from_s3_and_decrypt['./ab2d_sbx_keystore',"${env}-automation"]
  #
  # Get keystore from S3 and decrypt it
  export RUBY_BIN="/home/ec2-user/.rbenv/versions/2.6.5/bin"
  sudo "$RUBY_BIN/bundle" exec "$RUBY_BIN/rake" \
    get_file_from_s3_and_decrypt['./ab2d_sbx_keystore',"${env}-automation"]
  
  # Create a "bfd-keystore" directory under EFS (if doesn't exist)
  sudo mkdir -p "/mnt/efs/bfd-keystore/${env}"

  # Move the BFD keystore to the "bfd-keystore" directory
  sudo mv /tmp/ab2d_sbx_keystore "/mnt/efs/bfd-keystore/${env}"

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
