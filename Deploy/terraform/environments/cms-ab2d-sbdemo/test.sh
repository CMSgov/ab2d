#!/bin/bash

#
# Create base AWS networking
#

# Change to working directory
cd "$(dirname "$0")"

# Set environment
export AWS_PROFILE="sbdemo"
export DEBUG_LEVEL="WARN"
export ENVIRONMENT="sbdemo"
export KMS_KEY_ID="013df5de-ae3d-455b-81c3-26794bb68066"
export DATABASE_SECRET_DATETIME="2019-10-25-14-55-02"

#
# Get required user entry
#

# Get database user (if exists)
DATABASE_USER=$(./get-database-secret.py $ENVIRONMENT database_user $DATABASE_SECRET_DATETIME)

# Create database user secret (if doesn't exist)
if [ -z "${DATABASE_USER}" ]; then
  ./create-database-secret.py $ENVIRONMENT database_user $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  DATABASE_USER=$(./get-database-secret.py $ENVIRONMENT database_user $DATABASE_SECRET_DATETIME)
fi

# Get database password (if exists)
DATABASE_PASSWORD=$(./get-database-secret.py $ENVIRONMENT database_password $DATABASE_SECRET_DATETIME)

# Create database password secret (if doesn't exist)
if [ -z "${DATABASE_PASSWORD}" ]; then
  ./create-database-secret.py $ENVIRONMENT database_password $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  DATABASE_PASSWORD=$(./get-database-secret.py $ENVIRONMENT database_password $DATABASE_SECRET_DATETIME)
fi

# Get database name (if exists)
DATABASE_NAME=$(./get-database-secret.py $ENVIRONMENT database_name $DATABASE_SECRET_DATETIME)

# Create database name secret (if doesn't exist)
if [ -z "${DATABASE_NAME}" ]; then
  ./create-database-secret.py $ENVIRONMENT database_name $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  DATABASE_NAME=$(./get-database-secret.py $ENVIRONMENT database_name $DATABASE_SECRET_DATETIME)
fi

#
# Deploy db
#

terraform apply \
  --var "db_username=${DATABASE_USER}" \
  --var "db_password=${DATABASE_PASSWORD}" \
  --var "db_name=${DATABASE_NAME}" \
  --target module.db --auto-approve
