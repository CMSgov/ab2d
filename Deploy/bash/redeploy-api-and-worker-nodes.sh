#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Set default values
#

export DEBUG_LEVEL="WARN"

#
# Parse options
#

echo "Parse options..."
for i in "$@"
do
case $i in
  --profile=*)
  PROFILE="${i#*=}"
  shift # past argument=value
  ;;
  --environment=*)
  ENVIRONMENT="${i#*=}"
  CMS_ENV=$(echo $ENVIRONMENT | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
  --vpc-id=*)
  VPC_ID="${i#*=}"
  shift # past argument=value
  ;;
  --ssh-username=*)
  SSH_USERNAME="${i#*=}"
  shift # past argument=value
  ;;
  --owner=*)
  OWNER="${i#*=}"
  shift # past argument=value
  ;;
  --ec2-instance-type=*)
  EC2_INSTANCE_TYPE="${i#*=}"
  shift # past argument=value
  ;;
  --debug-level=*)
  DEBUG_LEVEL=$(echo ${i#*=} | tr '[:lower:]' '[:upper:]')
  shift # past argument=value
  ;;
  --build-new-images)
  BUILD_NEW_IMAGES="true"
  shift # past argument=value
  ;;
  --use-existing-images)
  USE_EXISTING_IMAGES="true"
  shift # past argument=value
  ;;
esac
done

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${ENVIRONMENT}" ] \
  || [ -z "${VPC_ID}" ] \
  || [ -z "${SSH_USERNAME}" ] \
  || [ -z "${OWNER}" ] \
  || [ -z "${EC2_INSTANCE_TYPE}" ]; then
    echo "Try running the script like one of these options:"
    echo "./redeploy-api-and-worker-nodes.sh --environment=dev"
    echo "./redeploy-api-and-worker-nodes.sh --environment=dev --debug-level=TRACE"
    echo "./redeploy-api-and-worker-nodes.sh --environment=dev --debug-level=DEBUG"
    echo "./redeploy-api-and-worker-nodes.sh --environment=dev --debug-level=INFO"
    echo "./redeploy-api-and-worker-nodes.sh --environment=dev --debug-level=WARN"
    echo "./redeploy-api-and-worker-nodes.sh --environment=dev --debug-level=ERROR"
    exit 1
fi

# Verify that VPC ID exists

VPC_EXISTS=$(aws --region us-east-1 ec2 describe-vpcs \
  --query "Vpcs[?VpcId=='$VPC_ID'].VpcId" \
  --output text)

if [ -z "${VPC_EXISTS}" ]; then
  echo "*********************************************************"
  echo "ERROR: VPC ID does not exist for the target AWS profile."
  echo "*********************************************************"
  exit 1
fi

#
# Configure terraform
#

# Reset logging

echo "Setting terraform debug level to $DEBUG_LEVEL..."
export TF_LOG=$DEBUG_LEVEL
export TF_LOG_PATH=/var/log/terraform/tf.log
rm -f /var/log/terraform/tf.log

#
# Set environment
#

export AWS_PROFILE=$(echo $PROFILE | tr '[:upper:]' '[:lower:]')

#
# Change to target environment directory
#

cd "${START_DIR}"
cd ../terraform/environments/$CMS_ENV

#
# Get environment settings
#

# Get first public subnet id

echo "Getting first public subnet id..."

SUBNET_PUBLIC_1_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID}" ]; then

    echo "ERROR: public subnet #1 not found..."
    exit 1

fi

# Get second public subnet id

echo "Getting second public subnet id..."

SUBNET_PUBLIC_2_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID}" ]; then

  echo "ERROR: public subnet #2 not found..."
  exit 1

fi

# Get first private subnet id

echo "Getting first private subnet id..."

SUBNET_PRIVATE_1_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID}" ]; then

  echo "ERROR: private subnet #1 not found..."
  exit 1
  
fi

# Get second private subnet id

echo "Getting second private subnet id..."

SUBNET_PRIVATE_2_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_ID}" ]; then

  echo "ERROR: private subnet #2 not found..."
  exit 1

fi

#
# Create ".auto.tfvars" file for the target environment
#

echo 'vpc_id = "'$VPC_ID'"' \
  > $CMS_ENV.auto.tfvars
echo 'private_subnet_ids = ["'$SUBNET_PRIVATE_1_ID'","'$SUBNET_PRIVATE_2_ID'"]' \
  >> $CMS_ENV.auto.tfvars
echo 'deployment_controller_subnet_ids = ["'$SUBNET_PUBLIC_1_ID'","'$SUBNET_PUBLIC_2_ID'"]' \
  >> $CMS_ENV.auto.tfvars
echo 'ec2_instance_type = "'$EC2_INSTANCE_TYPE'"' \
  >> $CMS_ENV.auto.tfvars
echo 'linux_user = "'$SSH_USERNAME'"' \
  >> $CMS_ENV.auto.tfvars
echo 'deployer_ip_address = "'$DEPLOYER_IP_ADDRESS'"' \
  >> $CMS_ENV.auto.tfvars

#
# Destroy the api and worker modules
#

# Get the Application Load Balancer (ALB) ARN

LOAD_BALANCERS_EXIST=$(aws --region us-east-1 elbv2 describe-load-balancers \
  --query "LoadBalancers[*].[LoadBalancerArn]" \
  --output text \
  | grep "${CMS_ENV}" \
  | xargs \
  | tr -d '\r')
if [ -n "${LOAD_BALANCERS_EXIST}" ]; then
  ALB_ARN=$(aws --region us-east-1 elbv2 describe-load-balancers \
    --name=$CMS_ENV \
    --query 'LoadBalancers[*].[LoadBalancerArn]' \
    --output text)
fi

# Turn off "delete protection" for the application load balancer

echo "Turn off 'delete protection' for the application load balancer..."
if [ -n "${ALB_ARN}" ]; then
  aws --region us-east-1 elbv2 modify-load-balancer-attributes \
    --load-balancer-arn $ALB_ARN \
    --attributes Key=deletion_protection.enabled,Value=false
fi

#  Detach AWS Shield standard from the application load balancer

# Note that no change is actually made since AWS shield standard remains applied to
# the application load balancer until the load balancer itself is removed. This section
# may be needed later if AWS Shield Advanced was applied instead.

echo "Destroying shield components..."
terraform destroy \
  --target module.shield \
  --auto-approve

# Destroy the environment of the "WAF" module

echo "Destroying WAF components..."
terraform destroy \
  --target module.waf \
  --auto-approve

# Destroy the environment of the "CloudWatch" module

echo "Destroying CloudWatch components..."
terraform destroy \
  --target module.cloudwatch \
  --auto-approve

# Destroy the environment of the "worker" module

echo "Destroying worker components..."
terraform destroy \
  --target module.worker \
  --auto-approve

# Destroy the environment of the "api" module

echo "Destroying API components..."
terraform destroy \
  --target module.api \
  --auto-approve

#
# Build or use existing images
#

if [ -n "${BUILD_NEW_IMAGES}" ]; then

  # Build and push API and worker to ECR

  echo "Build and push API and worker to ECR..."
  
  # Log on to ECR
      
  read -sra cmd < <(aws ecr get-login --no-include-email)
  pass="${cmd[5]}"
  unset cmd[4] cmd[5]
  "${cmd[@]}" --password-stdin <<< "$pass"

  # Build API and worker (if creating a new image)

  cd "${START_DIR}"
  cd ..
  make docker-build
  sleep 5

  # Build API docker image

  cd "${START_DIR}"
  cd ../api
  docker build \
    --tag "ab2d_api:latest" .

  # Build worker docker image

  cd "${START_DIR}"
  cd ../worker
  docker build \
    --tag "ab2d_worker:latest" .

  # Tag and push API docker image to ECR

  API_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_api'].repositoryUri" \
    --output text)
  if [ -z "${API_ECR_REPO_URI}" ]; then
    aws --region us-east-1 ecr create-repository \
        --repository-name "ab2d_api"
    API_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
      --query "repositories[?repositoryName == 'ab2d_api'].repositoryUri" \
      --output text)
  fi
  docker tag "ab2d_api:latest" "${API_ECR_REPO_URI}:latest"
  docker push "${API_ECR_REPO_URI}:latest"

  # Tag and push worker docker image to ECR

  WORKER_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
    --output text)
  if [ -z "${WORKER_ECR_REPO_URI}" ]; then
    aws --region us-east-1 ecr create-repository \
      --repository-name "ab2d_worker"
    WORKER_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
      --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
      --output text)
  fi
  docker tag "ab2d_worker:latest" "${WORKER_ECR_REPO_URI}:latest"
  docker push "${WORKER_ECR_REPO_URI}:latest"
   
else # use existing images

  echo "Using existing images..."

fi

#
# Redeply the api and worker modules
#

# *** TO DO ***
