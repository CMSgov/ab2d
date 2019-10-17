#!/bin/bash

systemctl start docker
sleep 60
wget -O vars.sh $MASTER_IP:777
eval $(cat vars.sh)
kubeadm join $MASTER_IP:6443 --token $TOKEN --discovery-token-ca-cert-hash $HASH
