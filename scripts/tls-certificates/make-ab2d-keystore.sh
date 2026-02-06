#!/bin/bash
set -e

# verify base64 encoded PKCS#12 file
#
function verify_encoded_p12 {
  rm -f foo.p12
  base64 --decode >foo.p12 <"$1"
  echo " "
  echo "verifying $1 keystore file..."
  openssl pkcs12 -in foo.p12 -password pass:changeit -nodes | openssl x509 -noout -enddate
  rm -f foo.p12
  echo "-------------------------------"
  echo " "
}

# Generate a keystore and a public key pem file
#
function gen_keystore {
  echo "begin processing of $3 environment...."
  rm -f "$3-keystore.pfx"
  rm -f "$3-keystore.pfx.b64"
  rm -f "$3-public-cert.pem"

  echo "Generating $3 keystore..."
  keytool -genkeypair \
    -alias server \
    -keyalg RSA \
    -keysize 4096 \
    -dname "$1" \
    -ext "$2" \
    -validity 730 \
    -keypass changeit \
    -keystore "$3-keystore.pfx" \
    -storepass changeit

  echo "Extracting public cert..."
  keytool -export -keystore "$3-keystore.pfx" -alias server -storepass changeit -file "$3-public-cert.pem" -rfc

  echo "creating base64 encoded version of PKCS#12 file..."
  cat "$3-keystore.pfx" | base64 >"$3-keystore.pfx.b64"

  verify_encoded_p12 "$3-keystore.pfx.b64"
}

# Prod
gen_keystore "cn=ab2d.cms.gov.local" \
  "san=dns:ab2d.cms.gov.local,dns:ab2d.cms.gov" \
  "prod"

# Prod-SBX
gen_keystore "cn=sandbox.ab2d.cms.gov.local" \
  "san=dns:sandbox.ab2d.cms.gov.local,dns:sandbox.ab2d.cms.gov" \
  "sandbox"

# Test
gen_keystore "cn=test.ab2d.cms.gov.local" \
  "san=dns:test.ab2d.cms.gov.local,dns:test.ab2d.cms.gov" \
  "test"

# Dev
gen_keystore "cn=dev.ab2d.cms.gov.local" \
  "san=dns:dev.ab2d.cms.gov.local,dns:dev.ab2d.cms.gov" \
  "dev"

echo
echo "REMEMBER: "
echo "* Store the contents of each <env>-keystore.pfx.b64 in SSM configuration under the /ab2d/<env>/worker/sensitive/mtls_keystore_base64 path"
echo "* Store the contents of each <env>-public-cert.pem in SSM configuration under the /ab2d/<env>/worker/sensitive/mtls_keystore_public_cert path"
