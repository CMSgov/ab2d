#!/bin/bash
set -e #Exit on first error
set -x #Be verbose

# Make sure dockerd is running
sudo systemctl start docker
sleep 60

# Initialize master
sudo kubeadm init --token-ttl 0

# Setup config
sudo mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# Enable weave networking
kubectl --kubeconfig=$HOME/.kube/config apply -f "https://cloud.weave.works/k8s/net?k8s-version=$(kubectl version | base64 | tr -d '\n')"

# Token and cert hash for joining cluster
TOKEN=$(sudo kubeadm token list | grep default | awk -F" " '{print $1}')
HASH=$(sudo openssl x509 -in /etc/kubernetes/pki/ca.crt -noout -pubkey | openssl rsa -pubin -outform DER 2>/dev/null | sha256sum | cut -d' ' -f1)
HASH="sha256:$HASH"
echo "export TOKEN=$TOKEN" > /tmp/vars.sh
echo "export HASH=$HASH" >> /tmp/vars.sh
sudo chmod 755 /tmp/vars.sh

# Serve token and hash out to nodes
echo '#!/bin/bash' > /tmp/server.sh
echo "trap '' HUP" >> /tmp/server.sh
echo "while true; do cat /tmp/vars.sh| /bin/nc -l 777 ; done" >> /tmp/server.sh
chmod +x /tmp/server.sh
sudo nohup /tmp/server.sh &
sleep 10
