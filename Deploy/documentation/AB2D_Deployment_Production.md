# AB2D Deployment Production

## Table of Contents

1. [Deploy to production](#deploy-to-production)

## Deploy to production

1. Ensure that you are connected to CMS Cisco VPN

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```
   
1. Initialize environment

   ```ShellShession
   $ ./bash/
   ```

1. Create or update gold disk

   ```ShellSession
   ```
   
1. Deploy infrastructure

   ```ShellSession
   $ ./deploy-infrastructure.sh \
     --environment=ab2d-east-prod \
     --ecr-repo-environment=ab2d-mgmt-east-dev \
     --region=us-east-1 \
     --ssh-username=ec2-user \
     --owner=743302140042 \
     --ec2_instance_type_api=m5.xlarge \
     --ec2_instance_type_worker=m5.xlarge \
     --ec2_instance_type_other=m5.xlarge \
     --ec2_desired_instance_count_api=1 \
     --ec2_minimum_instance_count_api=1 \
     --ec2_maximum_instance_count_api=1 \
     --ec2_desired_instance_count_worker=1 \
     --ec2_minimum_instance_count_worker=1 \
     --ec2_maximum_instance_count_worker=1 \
     --database-secret-datetime=2020-01-02-09-15-01 \
     --build-new-images \
     --internet-facing=false \
     --auto-approve
   ```

