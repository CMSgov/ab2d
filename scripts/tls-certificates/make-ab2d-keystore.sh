#!/opt/homebrew/bin/bash
#above, shell location for BASH version 4+
# Environment Password Prompt Script

set -euo pipefail  # Exit on error, undefined vars, pipe failures

# Declare associative array to store passwords
declare -A PASSWORDS

# Display header
print_header() {
    echo "================================================"
    echo "  MTLS Key/Cert Generation by Environment"
    echo "================================================"
    echo ""
}

# Prompt for environment
prompt_environment() {
PS3="Select an environment: "
options=("dev" "test" "sandbox" "prod")

select opt in "${options[@]}"
do
    case $opt in
        "dev")
            env="dev"
            break
            ;;
        "test")
            env="test"
            break
            ;;
        "prod")
            env="prod"
            break
            ;;
        "sandbox")
            env="sandbox"
            break
            ;;
        *)
            echo "Invalid option $REPLY"
            ;;
    esac
done

echo "You selected: $env"
}

# Prompt for password with validation
prompt_password() {
    local password password_confirm

    echo -e "Enter password for ${env^^} environment:"
    read -rs -p "Password: " password
    echo ""

    read -rs -p "Confirm password: " password_confirm
    echo ""

    # Validate passwords match
    if [[ "$password" != "$password_confirm" ]]; then
        echo -e "Error: Passwords do not match for ${env}. Exiting." >&2
        exit 1
    fi

    # Validate password is not empty
    if [[ -z "$password" ]]; then
        echo -e "Error: Password cannot be empty for ${env}. Exiting." >&2
        exit 1
    fi

    PASSWORDS[$env]="$password"
    echo -e "✓ Password for ${env} set successfully"
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
        echo -e "Error: Failed to verify keystore for $env" >&2
        return 1
    fi
}

# Generate a keystore and a public key pem file
gen_keystore() {
    local dname="$1"
    local env="$2"
    local password="${PASSWORDS[$env]}"

    echo "Begin processing of $env environment..."

    # Clean up existing files
    rm -f "${env}-keystore.pfx" "${env}-keystore.pfx.b64" "${env}-public-cert.pem"

    echo "Generating $env keystore..."
    keytool -genkeypair \
        -alias worker \
        -keyalg RSA \
        -keysize 4096 \
        -dname "$dname" \
        -validity 730 \
        -keypass "$password" \
        -keystore "${env}-keystore.pfx" \
        -storepass "$password"

    echo "Extracting public cert..."
    keytool -export \
        -keystore "${env}-keystore.pfx" \
        -alias worker \
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

    prompt_environment
    prompt_password "$env"

    gen_keystore "cn=ab2d-$env" \
        "$env"

    # Display instructions
    cat <<EOF

REMEMBER:
* Store the contents of each <env>-keystore.pfx.b64 in SSM configuration under the /ab2d/${env}/worker/sensitive/bfd_keystore_base64 path
* Store the contents of each <env>-public-cert.pem in SSM configuration under the /ab2d/${env}/common/nonsensitive/bfd-client/mtls-public-cert path
EOF
}

main "$@"
