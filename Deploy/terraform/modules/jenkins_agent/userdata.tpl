#!/bin/bash

#
# Set more useful hostname
#

echo "$(hostname -s).${env}" > /tmp/hostname
sudo mv /tmp/hostname /etc/hostname
sudo hostname "$(hostname -s).${env}"
