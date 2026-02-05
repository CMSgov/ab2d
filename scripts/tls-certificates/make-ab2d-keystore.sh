#!/opt/homebrew/bin/bash
#above, shell location for BASH version 4+

# Environment Password Prompt Script
# Securely prompts for passwords for multiple environments

set -euo pipefail  # Exit on error, undefined vars, pipe failures

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly NC='\033[0m'

# Environments
readonly ENVIRONMENTS=("dev" "test" "sandbox" "prod")

# Declare associative array to store passwords
declare -A PASSWORDS

# Display header
print_header() {
    echo "================================================"
    echo "  Environment Password Configuration"
    echo "================================================"
    echo ""
}

# Prompt for password with validation
prompt_password() {
    local env="$1"
    local password password_confirm

    echo -e "${YELLOW}Enter password for ${env^^} environment:${NC}"
    read -rs -p "Password: " password
    echo ""

    read -rs -p "Confirm password: " password_confirm
    echo ""

    # Validate passwords match
    if [[ "$password" != "$password_confirm" ]]; then
        echo -e "${RED}Error: Passwords do not match for ${env}. Exiting.${NC}" >&2
        exit 1
    fi

    # Validate password is not empty
    if [[ -z "$password" ]]; then
        echo -e "${RED}Error: Password cannot be empty for ${env}. Exiting.${NC}" >&2
        exit 1
    fi

    PASSWORDS[$env]="$password"
    echo -e "${GREEN}✓ Password for ${env} set successfully${NC}"
    echo ""
}

# Verify base64 encoded PKCS#12 file
verify_encoded_p12() {
    local encoded_file="$1"
    local env="$2"
    local temp_file

    temp_file=$(mktemp)
    trap "rm -f '$temp_file'" RETURN

    base64 --decode < "$encoded_file" > "$temp_file"

    echo ""
    echo "Verifying $encoded_file keystore file..."
    if openssl pkcs12 -in "$temp_file" -password "pass:${PASSWORDS[$env]}" -nodes 2>/dev/null | \
       openssl x509 -noout -enddate; then
        echo "-------------------------------"
        echo ""
    else
        echo -e "${RED}Error: Failed to verify keystore for $env${NC}" >&2
        return 1
    fi
}

# Generate a keystore and a public key pem file
gen_keystore() {
    local dname="$1"
    local san="$2"
    local env="$3"
    local password="${PASSWORDS[$env]}"

    echo "Begin processing of $env environment..."

    # Clean up existing files
    rm -f "${env}-keystore.pfx" "${env}-keystore.pfx.b64" "${env}-public-cert.pem"

    echo "Generating $env keystore..."
    keytool -genkeypair \
        -alias server \
        -keyalg RSA \
        -keysize 4096 \
        -dname "$dname" \
        -ext "$san" \
        -validity 730 \
        -keypass "$password" \
        -keystore "${env}-keystore.pfx" \
        -storepass "$password"

    echo "Extracting public cert..."
    keytool -export \
        -keystore "${env}-keystore.pfx" \
        -alias server \
        -storepass "$password" \
        -file "${env}-public-cert.pem" \
        -rfc

    echo "Creating base64 encoded version of PKCS#12 file..."
    base64 < "${env}-keystore.pfx" > "${env}-keystore.pfx.b64"

    verify_encoded_p12 "${env}-keystore.pfx.b64" "$env"
}

# Main execution
main() {
    print_header

    # Prompt for all passwords
    for env in "${ENVIRONMENTS[@]}"; do
        prompt_password "$env"
    done

    # Generate keystores for each environment
    gen_keystore "cn=ab2d.cms.gov.local" \
        "san=dns:ab2d.cms.gov.local,dns:ab2d.cms.gov" \
        "prod"

    gen_keystore "cn=sandbox.ab2d.cms.gov.local" \
        "san=dns:sandbox.ab2d.cms.gov.local,dns:sandbox.ab2d.cms.gov" \
        "sandbox"

    gen_keystore "cn=test.ab2d.cms.gov.local" \
        "san=dns:test.ab2d.cms.gov.local,dns:test.ab2d.cms.gov" \
        "test"

    gen_keystore "cn=dev.ab2d.cms.gov.local" \
        "san=dns:dev.ab2d.cms.gov.local,dns:dev.ab2d.cms.gov" \
        "dev"

    # Display instructions
    cat <<EOF

REMEMBER:
* Store the contents of each <env>-keystore.pfx.b64 in SSM configuration under the /ab2d/${env}/worker/sensitive/mtls_keystore_base64 path
* Store the contents of each <env>-public-cert.pem in SSM configuration under the /ab2d/${env}/common/nonsensitive/bfd-client/mtls-public-cert path
EOF
}

main "$@"
