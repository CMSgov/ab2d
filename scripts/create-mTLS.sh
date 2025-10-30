#!/bin/bash

# Generate CA
openssl req -x509 -newkey rsa:4096 -nodes \
  -keyout ca-key-prod-ab2d.pem -out ca-cert-prod-ab2d.pem -days 365 \
  -subj "/C=US/ST=Maryland/L=Baltimore/O=Centers for Medicare and Medicaid Services/CN=prod.ab2d.cms.gov" \
  -addext "basicConstraints=critical,CA:true" \
  -addext "keyUsage=critical,digitalSignature,cRLSign,keyCertSign"

# Generate Server Certificate
openssl req -newkey rsa:4096 -nodes \
  -keyout prod-ab2d-server-key.pem -out server.csr \
  -subj "/C=US/ST=Virginia/L=Reston/O=CMS/CN=localhost"

openssl x509 -req -days 365 \
  -in server.csr -CA ca-cert-prod-ab2d.pem -CAkey ca-key-prod-ab2d.pem -CAcreateserial \
  -out prod-ab2d-server-cert.pem \
  -copy_extensions copyall \
  -extfile <(cat <<EOF
basicConstraints=CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:localhost,DNS:*.example.com,IP:127.0.0.1
EOF
)

# Generate Client Certificate
openssl req -newkey rsa:4096 -nodes \
  -keyout prod-ab2d-client-key.pem -out client.csr \
  -subj "/C=US/ST=Maryland/L=Baltimore/O=Centers for Medicare and Medicaid Services/CN=prod.ab2d.cms.gov-client1"

openssl x509 -req -days 365 \
  -in client.csr -CA ca-cert-prod-ab2d.pem -CAkey ca-key-prod-ab2d.pem -CAcreateserial \
  -out prod-ab2d-client-cert.pem \
  -copy_extensions copyall \
  -extfile <(cat <<EOF
basicConstraints=CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=clientAuth
subjectAltName=DNS:client1.example.com
EOF
)

rm server.csr client.csr