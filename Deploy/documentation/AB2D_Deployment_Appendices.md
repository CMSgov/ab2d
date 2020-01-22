# AB2D Deployment Appendices

## Table of Contents

1. [Appendix A: Access the CMS AWS console](#appendix-a-access-the-cms-aws-console)
1. [Appendix B: View the current gold disk images](#appendix-b-view-the-current-gold-disk-images)
1. [Appendix C: Interact with the developer S3 bucket](#appendix-c-interact-with-the-developer-s3-bucket)
1. [Appendix D: Delete and recreate IAM instance profiles, roles, and policies](#appendix-d-delete-and-recreate-iam-instance-profiles-roles-and-policies)
   * [Delete instance profiles](#delete-instance-profiles)
   * [Delete roles](#delete-roles)
   * [Delete policies not used by IAM users](#delete-policies-not-used-by-iam-users)
1. [Appendix E: Interacting with IAM policy versions](#appendix-e-interacting-with-iam-policy-versions)
1. [Appendix F: Interacting with Elastic Container Repository](#appendix-f-interacting-with-elastic-container-repository)
1. [Appendix G: Generate and test the AB2D website](#appendix-g-generate-and-test-the-ab2d-website)
1. [Appendix H: Get log file from a node](#appendix-h-get-log-file-from-a-node)
1. [Appendix I: Connect to a running container](#appendix-i-connect-to-a-running-container)
1. [Appendix J: Delete and recreate database](#appendix-j-delete-and-recreate-database)
1. [Appendix K: Complete DevOps linting checks](#appendix-k-complete-devops-linting-checks)
   * [Complete terraform linting](#complete-terraform-linting)
   * [Complete python linting](#complete-python-linting)
1. [Appendix L: View existing EUA job codes](#appendix-l-view-existing-eua-job-codes)
1. [Appendix M: Make AB2D static website unavailable](#appendix-m-make-ab2d-static-website-unavailable)
   * [Delete the contents of the websites S3 bucket](#delete-the-contents-of-the-websites-s3-bucket)
   * [Delete the cached website files from the CloudFront edge caches before they expire](#delete-the-cached-website-files-from-the-cloudfront-edge-caches-before-they-expire)
1. [Appendix N: Destroy and redploy API and Worker nodes](#appendix-n-destroy-and-redploy-api-and-worker-nodes)
1. [Appendix O: Destroy complete environment](#appendix-o-destroy-complete-environment)
1. [Appendix P: Display disk space](#appendix-p-display-disk-space)
1. [Appendix Q: Test API using swagger](#appendix-q-test-api-using-swagger)
1. [Appendix R: Update userdata for auto scaling groups through the AWS console](#appendix-r-update-userdata-for-auto-scaling-groups-through-the-aws-console)

## Appendix A: Access the CMS AWS console

1. Log into Cisco AnyConnect client

   1. Select **Launchpad**
   
   1. Select **Cisco AnyConnect Secure Mobility Client**
   
   1. If a "Cisco AnyConnect Secure Mobility Client is not optimized for your Mac and needs to be updated" message appears, do the following:
      
      1. Select **OK**
   
      1. See the following for more information
   
         > https://support.apple.com/en-us/HT208436
   
   1. Enter the following in the **VPN** dropdown
   
      ```
      cloudvpn.cms.gov
      ```
   
   1. Select **Connect**
   
   1. Enter the following on the "Please enter your username and password" window
   
      - **USERNAME:** {your eua id}
   
      - **PASSWORD:** {password for CloudTamer and Cloud VPN}
   
      - **2nd Password:** {latest 6 number from the google authentication app on mobile phone}
   
   1. Select **OK**
   
   1. Select **Accept**
   
   1. If a "vpndownloader is not optimized for your Mac and needs to be updated" message appears, do the following:
      
      1. Select **OK**
   
      1. See the following for more information
   
         > https://support.apple.com/en-us/HT208436
   
   1. Verify that the VPN successfully connects

1. Log on to CloudTamer

   1. Open Chrome

   1. Enter the following in the address bar

      > https://cloudtamer.cms.gov

   1. Enter the following on the CloudTamer logon page

      - **Username:** {your eua id}

      - **Password:** {password for CloudTamer and Cloud VPN}

      - **Dropdown:** CMS Cloud Services

   1. Select **Log In**

1. Select **Projects** from the leftmost panel

1. Select **Filters**

1. Type the following in the **By Keyword** text box

   ab2d

1. Select **Apply Filters**

1. Select **Account Access** beside "Project AB2D"

1. Select the account
   
   ```
   aws-cms-oit-iusg-acct66 (349849222861)
   ```

1. Select the cloud access role

   
   ```
   West-AB2D
   ```

1. Select **Web Access**

1. Note that the AWS console for the AB2D project should now appear

## Appendix B: View the current gold disk images

1. Open Chrome

1. Enter the following in the address bar

   > https://github.cms.gov/CCSVDC/gold-image/blob/master/ami_latest.txt

## Appendix C: Interact with the developer S3 bucket

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Verify that the "cms-ab2d-dev" bucket exists

   ```ShellSession
   $ aws --region us-east-1 s3api list-buckets \
     --query "Buckets[?Name=='cms-ab2d-dev'].Name" \
     --output text
   ```

1. Create a text file as an example (optional)

   ```ShellSession
   $ echo "test" > opt-out.txt
   ```
   
1. Put a file in the root of the bucket

   *Example using a file named opt-out.txt exists in the current directory:*
   
   ```ShellSession
   $ aws --region us-east-1 s3api put-object \
     --bucket cms-ab2d-dev \
     --key opt-out.txt \
     --body ./opt-out.txt
   ```

1. List objects in the root of the bucket

   ```ShellSession
   $ aws --region us-east-1 s3api list-objects \
     --bucket cms-ab2d-dev \
     --query 'Contents[].Key' \
     --output text
   ```

1. Get a file from the root of the bucket

   ```ShellSession
   $ aws --region us-east-1 s3api get-object \
     --bucket cms-ab2d-dev \
     --key opt-out.txt \
     ./opt-out-copy.txt
   ```

1. Delete a file from the root of the bucket

   ```ShellSession
   $ aws --region us-east-1 s3api delete-object \
     --bucket cms-ab2d-dev \
     --key opt-out.txt
   ```

1. Verify that the object is gone by listing objects in the root of the bucket again

   ```ShellSession
   $ aws --region us-east-1 s3api list-objects \
     --bucket cms-ab2d-dev \
     --query 'Contents[].Key' \
     --output text
   ```

## Appendix D: Delete and recreate IAM instance profiles, roles, and policies

### Delete instance profiles

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Detach "Ab2dInstanceRole" from "Ab2dInstanceProfile"

   ```ShellSession
   $ aws iam remove-role-from-instance-profile \
     --role-name Ab2dInstanceRole \
     --instance-profile-name Ab2dInstanceProfile
   ```

1. Delete instance profile

   ```ShellSession
   $ aws iam delete-instance-profile \
     --instance-profile-name Ab2dInstanceProfile
   ```

### Delete roles

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Detach policies from the "Ab2dManagedRole" role

   ```ShellSession
   $ aws iam detach-role-policy \
     --role-name Ab2dManagedRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dAccessPolicy
   ```

1. Delete "Ab2dManagedRole" role

   ```ShelSession
   $ aws iam delete-role \
     --role-name Ab2dManagedRole
   ```

1. Detach policies from the "Ab2dInstanceRole" role

   ```ShellSession
   $ aws iam detach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dAssumePolicy
   $ aws iam detach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dPackerPolicy
   $ aws iam detach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dS3AccessPolicy
   $ aws iam detach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role
   ```

1. Delete "Ab2dInstanceRole" role

   ```ShelSession
   $ aws iam delete-role \
     --role-name Ab2dInstanceRole
   ```

### Delete policies not used by IAM users

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Delete "Ab2dAccessPolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dAccessPolicy
   ```

1. Delete "Ab2dAssumePolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dAssumePolicy
   ```

1. Delete "Ab2dPackerPolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dPackerPolicy
   ```

1. Delete "Ab2dS3AccessPolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dS3AccessPolicy
   ```

## Appendix E: Interacting with IAM policy versions

1. List policy versions

   1. Enter the following
   
      *Example of listing policy versions of the "Ab2dAssumePolicy" policy:*
   
      ```ShellSession
      $ aws iam list-policy-versions \
        --policy-arn arn:aws:iam::114601554524:policy/Ab2dAssumePolicy
      ```

   1. Examine the output

      *Example of listing policy versions of the "Ab2dAssumePolicy" policy:*
      
      ```
      {
          "Versions": [
              {
                  "VersionId": "v2",
                  "IsDefaultVersion": true,
                  "CreateDate": "2019-10-11T23:23:54Z"
              },
              {
                  "VersionId": "v1",
                  "IsDefaultVersion": false,
                  "CreateDate": "2019-10-04T19:11:21Z"
              }
          ]
      }
      ```
   
1. Delete a policy version

   *Example of deleting "v1" of the "Ab2dAssumePolicy" policy:*

   ```ShellSession
   $ aws iam delete-policy-version \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dAssumePolicy \
     --version-id v1
   ```

## Appendix F: Interacting with Elastic Container Repository

1. List ECR repositories

   ```ShellSession
   $ aws --region us-east-1 ecr describe-repositories \
     --query "repositories[*].repositoryName" \
     --output text
   ```

1. Delete an ECR repository

   *Example deleting a repository named "ab2d_sbdemo-dev_api":*
   
   ```ShellSession
   $ aws --region us-east-1 ecr delete-repository \
     --repository-name ab2d_sbdemo-dev_api \
     --force
   ```

## Appendix G: Generate and test the AB2D website

1. Change to the "website" directory

   ```ShellSession
   $ cd ~/code/ab2d/website
   ```

1. Generate and test the website

   1. Ensure required gems are installed

      ```ShellSession
      $ bundle install
      ```

   1. Generate and serve website on the jekyll server

      ```ShellSession
      $ bundle exec jekyll serve
      ```
     
   1. Open Chrome

   1. Enter the following in the address bar
   
      > http://127.0.0.1:4000
      
   1. Verify that the website comes up

   1. Return to the terminal where the jekyll server is running
   
   1. Press **control+c** on the keyboard to stop the Jekyll server

1. Verify the generated site

   1. Note that a "_site" directory was automatically generated when you ran "bundle exec jekyll serve"
   
   1. List the contents of the directory

      ```ShellSession
      $ ls _site
      ```
    
   1. Note that the following two files are used as part of configuring the website within S3

      - index.html

      - 404.html

## Appendix H: Get log file from a node

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

1. Set target environment

   *Example for "Dev" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   ```

1. Set controller access variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export CONTROLLER_PUBLIC_IP=52.7.241.208
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ export CONTROLLER_PUBLIC_IP=3.93.125.65
   $ export SSH_USER_NAME=ec2-user
   ```

1. Copy the key to the controller

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}:~/.ssh
   ```
   
1. Connect to the controller

   *Format:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
   ```

1. Set node variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   $ export NODE_PRIVATE_IP=10.242.26.231
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment (api node 1):*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   $ export NODE_PRIVATE_IP=10.242.31.151
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment (api node 2):*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   $ export NODE_PRIVATE_IP=10.242.31.120
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment (worker node 1):*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   $ export NODE_PRIVATE_IP=10.242.31.153
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment (worker node 2):*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   $ export NODE_PRIVATE_IP=10.242.31.25
   $ export SSH_USER_NAME=ec2-user
   ```

1. Connect to a node

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${NODE_PRIVATE_IP}
   ```
   
1. Copy "messages" log to ec2-user home directory

   ```ShellSession
   $ sudo su
   $ cp /var/log/messages /home/ec2-user
   $ chown ec2-user:ec2-user /home/ec2-user/messages
   $ exit
   ```

1. Log off node

   ```ShellSession
   $ logout
   ```

1. Copy messages file to the controller

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${NODE_PRIVATE_IP}:~/messages .
   ```

1. Log off controller

   ```ShellSession
   $ logout
   ```

1. Copy messages file to development machine

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}:~/messages ~/Downloads
   ```

1. Note that the messages file from the remote node can now be found here

   ```
   ~/Downloads/messages
   ```

## Appendix I: Connect to a running container

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

1. Set target environment

   *Example for "Dev" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   ```

1. Set controller access variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export CONTROLLER_PUBLIC_IP=52.7.241.208
   $ export SSH_USER_NAME=ec2-user
   ```

1. Copy the key to the controller

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}:~/.ssh
   ```
   
1. Connect to the controller

   *Format:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
   ```

1. Set node variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   $ export NODE_PRIVATE_IP=10.242.26.231
   $ export SSH_USER_NAME=ec2-user
   ```
   
1. Connect to a node

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${NODE_PRIVATE_IP}
   ```

1. Connect to a running container

   *Example for connecting to an API container:*

   ```ShellSession
   $ docker exec -it $(docker ps -aqf "name=ecs-api-*" --filter "status=running") /bin/bash
   ```

   *Example for connecting to a worker container:*

   ```ShellSession
   $ docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") /bin/bash
   ```

## Appendix J: Delete and recreate database

1. Set the target AWS profile

   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Set controller access variables

   *Example for CMS development environment:*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-shared
   $ CONTROLLER_PUBLIC_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PublicIpAddress" \
     --output text)
   $ export SSH_USER_NAME=ec2-user
   ```
   
1. Connect to the controller

   *Format:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
   ```

1. Set target DB environment variables

   *Format:*
   
   ```ShellSession
   $ export MAIN_DB_NAME=postgres
   $ export TARGET_DB_NAME={target database name}
   $ export DB_HOST={database host}
   $ export DB_USER={database user}
   ```

1. Terminate all connections

   ```ShellSession
   $ psql --host  "${DB_HOST}" \
       --username "${DB_USER}" \
       --dbname "${MAIN_DB_NAME}" \
       --command "SELECT pg_terminate_backend(pg_stat_activity.pid) \
         FROM pg_stat_activity \
         WHERE pg_stat_activity.datname = '${TARGET_DB_NAME}' \
         AND pid <> pg_backend_pid();"
   ```

1. Delete the database

   ```ShellSession
   $ dropdb ${TARGET_DB_NAME} --host ${DB_HOST} --username ${DB_USER}
   ```

1. Create database

   ```ShellSession
   $ createdb ${TARGET_DB_NAME} --host ${DB_HOST} --username ${DB_USER}
   ```

## Appendix K: Complete DevOps linting checks

### Complete terraform linting

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Do a linting check

   ```ShellSession
   $ ./bash/tflint-check.sh
   ```

### Complete python linting

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Do a linting check

   ```ShellSession
   $ flake8 ./python3 > ~/Downloads/python3-linting.txt
   ```

1. Open the "python3-linting.txt" file and resolve any issues listed

   ```ShellSession
   $ vim ~/Downloads/python3-linting.txt
   ```

## Appendix L: View existing EUA job codes

1. Open Chrome

1. Enter the following in the address bar

   > https://eua.cms.gov/iam/im/pri/
   
1. Log on to EUA

   *Note that you should use the same username and password as you use for Jira and Confluence.*

1. Expand **EUA Info** in the leftmost panel

1. Select **Job Code Listing**

1. Select the following in the **Search for a group** section

   *Example searching for job codes with "SPLUNK" in the name:*
   
   ```
   where  Job Code  = *SPLUNK*	
   ```

## Appendix M: Make AB2D static website unavailable

### Delete the contents of the websites S3 bucket

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Delete all files from the "cms-ab2d-website" S3 bucket

   ```ShellSession
   $ aws --region us-east-1 s3 rm s3://cms-ab2d-website \
     --recursive
   ```

### Delete the cached website files from the CloudFront edge caches before they expire

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Invalidate the cloud distribution

   *Format:*
   
   ```ShellSession
   $ aws --region us-east-1 cloudfront create-invalidation \
     --distribution-id {cloudfront distribution id} \
     --paths "/*
   ```

   *Example:*
   
   ```ShellSession
   $ aws --region us-east-1 cloudfront create-invalidation \
     --distribution-id E8P2KHG7IH0TG \
     --paths "/*
   ```

1. Note the output

   *Format:*
   
   ```
   {
       "Location": "https://cloudfront.amazonaws.com/2019-03-26/distribution/{distribution id}/invalidation/{invalidation id}",
       "Invalidation": {
           "Id": "IKBNBOOJNM5OL",
           "Status": "InProgress",
           "CreateTime": "2019-12-16T15:22:16.488Z",
           "InvalidationBatch": {
               "Paths": {
                   "Quantity": 1,
                   "Items": [
                       "/*"
                   ]
               },
               "CallerReference": "cli-{caller reference id}"
           }
       }
   }
   ```

1. Note that the status says "InProgress"

1. Wait a few minutes to all the invalidation process to complete

1. Verify that the invalidation has completed

   1. Enter the following

      *Format:*
      
      ```ShellSession
      $ aws --region us-east-1 cloudfront list-invalidations \
        --distribution-id {cloudfront distribution id}
      ```

      *Example:*
      
      ```ShellSession
      $ aws --region us-east-1 cloudfront list-invalidations \
        --distribution-id E8P2KHG7IH0TG
      ```

   1. Verify that "Status" is "Completed"

      *Example:*
      
      ```
      {
          "InvalidationList": {
              "Items": [
                  {
                      "Id": "IKBNBOOJNM5OL",
                      "CreateTime": "2019-12-16T15:22:16.488Z",
                      "Status": "Completed"
                  }
              ]
          }
      }
      ```

1. Note that the invalidation will remain under the "Invalidations" tab for auditing purposes even if you redeploy the website

## Appendix N: Destroy and redploy API and Worker nodes

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Destroy and redploy API and Worker nodes

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ ./bash/redeploy-api-and-worker-nodes.sh \
     --profile=ab2d-dev \
     --environment=dev \
     --vpc-id=vpc-0c6413ec40c5fdac3 \
     --ssh-username=ec2-user \
     --owner=842420567215 \
     --ec2-instance-type=m5.xlarge \
     --debug-level=WARN
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ ./bash/redeploy-api-and-worker-nodes.sh \
     --profile=ab2d-sbx-sandbox \
     --environment=sbx-sandbox \
     --vpc-id=vpc-08dbf3fa96684151c \
     --ssh-username=ec2-user \
     --owner=842420567215 \
     --ec2-instance-type=m5.xlarge \
     --debug-level=WARN
   ```

## Appendix O: Destroy complete environment

1. Change to the deploy directory

   *Format:*
   
   ```ShellSession
   $ cd {code directory}/ab2d/Deploy
   ```

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```
   
1. Destroy the "dev" environment

   *Example to destroy the Dev environment:*
   
   ```ShellSession
   $ ./bash/destroy-environment.sh \
     --environment=ab2d-dev \
     --shared-environment=ab2d-dev-shared
   ```

   *Example to destroy the Dev environment, but preserve the AMIs:*
   
   ```ShellSession
   $ ./bash/destroy-environment.sh \
     --environment=ab2d-dev \
     --shared-environment=ab2d-dev-shared \
     --keep-ami
   ```

1. Destroy the "sbx" environment

   *Example to destroy the Sbx environment:*

   ```ShellSession
   $ ./bash/destroy-environment.sh \
     --environment=ab2d-sbx-sandbox \
     --shared-environment=ab2d-sbx-sandbox-shared
   ```

   *Example to destroy the Sbx environment, but preserve the AMIs:*
   
   ```ShellSession
   $ ./bash/destroy-environment.sh \
     --environment=ab2d-sbx-sandbox \
     --shared-environment=ab2d-sbx-sandbox-shared \
     --keep-ami
   ```

## Appendix P: Display disk space

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

1. Set target environment

   *Example for "Dev" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   ```

1. Set controller access variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export CONTROLLER_PUBLIC_IP=52.7.241.208
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ export CONTROLLER_PUBLIC_IP=3.93.125.65
   $ export SSH_USER_NAME=ec2-user
   ```

1. Copy the key to the controller

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}:~/.ssh
   ```
   
1. Connect to the controller
   
   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
   ```

1. Set node variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   $ export NODE_PRIVATE_IP=10.242.31.196
   $ export SSH_USER_NAME=ec2-user
   ```
   
1. Connect to node

   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${NODE_PRIVATE_IP}
   ```

1. Display the amount of disk space available on each file system in a human readable manner

   1. Enter the following

      ```ShellSession
      $ sudo df -h
      ```

   1. Note the output

      ```
      Filesystem                        Size  Used Avail Use% Mounted on
      devtmpfs                          7.6G     0  7.6G   0% /dev
      tmpfs                             7.6G   16K  7.6G   1% /dev/shm
      tmpfs                             7.6G   57M  7.5G   1% /run
      tmpfs                             7.6G     0  7.6G   0% /sys/fs/cgroup
      /dev/mapper/VolGroup00-rootVol     10G  2.4G  7.7G  24% /
      /dev/nvme0n1p1                   1014M  275M  740M  28% /boot
      /dev/mapper/VolGroup00-homeVol    3.0G   33M  3.0G   2% /home
      /dev/mapper/VolGroup00-tmpVol    1014M   34M  981M   4% /tmp
      /dev/mapper/VolGroup00-varVol     6.0G  1.8G  4.3G  30% /var
      /dev/mapper/VolGroup00-vartmpVol 1014M   33M  982M   4% /var/tmp
      /dev/mapper/VolGroup00-logVol     4.0G   43M  4.0G   2% /var/log
      /dev/mapper/VolGroup00-auditVol   4.0G   53M  4.0G   2% /var/log/audit
      overlay                           6.0G  1.8G  4.3G  30% /var/lib/docker/overlay2/806c002b8d5a7e51925854155c7f40808d6501908f17e58560a78e8e70a7d521/merged
      shm                                64M     0   64M   0% /var/lib/docker/containers/831e3b41737ee1174af0a93aa991543831565f3a523f62c35e026661d818bf7d/mounts/shm
      overlay                           6.0G  1.8G  4.3G  30% /var/lib/docker/overlay2/a608f39b66941b6333aaf7b6b1e0346f42f5f1c160a26528a4382da2b4fcb774/merged
      shm                                64M     0   64M   0% /var/lib/docker/containers/617e4a4ec4195e1f47d8e6e4c4e743eeb2d60c26309e42598d318fc53da9dd08/mounts/shm
      tmpfs                             1.6G     0  1.6G   0% /run/user/1000
      ```

## Appendix Q: Test API using swagger

1. Open Chrome

1. Enter the following in the address bar

   > https://confluence.cms.gov/display/AB2D/Step+By+Step+Tutorial

1. Note the following information from this document

   - Client Id

   - Client Password

   - username

   - password

   - OKTA server URL
   
1. Open Postman

1. Create an "ab2d" collection

   1. Select "New"

   1. Select "Collection"

   1. Enter the following on the "CREATE A NEW COLLECTION" page

      - **Name:** ab2d

   1. Select "Create"

1. Add an "retrieve-a-token" request to the "ab2d" collection

   1. Select **...** beside the "ab2d" collection

   1. Select **Add Request**

   1. Enter the following on the "SAVE REQUEST" page

      - **Request name:** retreive-a-token

   1. Select **Save to ab2d**

1. Configure the "retreive-a-token" request

   1. Expand the "ab2d" collection node

   1. Select the following

      ```
      GET retreive-a-token
      ```

   1. Change "GET" to "POST"

   1. Enter the noted OKTA server URL in the "Enter request URL" text box

   1. Select the **Params** tab

   1. Add the following key value pairs

      Key       |Value
      ----------|----------
      grant_type|password
      scope     |openid
      username  |{noted username}
      password  |{noted password}

   1. Note that add parameters now appear as part of the OKTA server URL

   1. Select the **Headers** tab

   1. Add the following key value pairs

      Key         |Value
      ------------|----------
      Content-Type|application/x-www-form-urlencoded
      Accept      |application/json
   
   1. Select the **Authorization** tab

   1. Select "Basic Auth" from the "TYPE" dropdown

   1. Configure the "Authorization" page

      - **Username:** {noted username}

      - **Password:** {noted password}

   1. Select **Send**

   1. Verify that you get an "access_token" within the JSON response

   1. Note the "access_token"

   1. Note that this "access_token" is the JWT access token that you will use within the swagger-ui)

   1. Note that you can get a cURL version of the request by doing the following

      1. Select **Code** to the far right of the tabbed options (Params, Authorization, etc.)

      1. Select **cURL** from the leftmost panel of the "GENERATE CODE SNIPPETS" page

      1. Note the "cURL" statement

1. Open Chrome

1. Enter the "swagger-ui.html" URL for the target environment in the address bar

1. Select **bulk-data-access-api**

1. Note the list of API endpoints that are displayed

1. Authorize the endpoints using the JWT access token

   1. Select **Authorize**

   1. Paste the JWT access token value in the **Value** text box using the following format

      *Format:*

      ```
      Bearer {jwt access token}
      ```

   1. Select **Authorize**

   1. Verify that "Authorized" is displayed under "Authorization (apiKey)"

   1. Select **Close**

   1. Verify that all the lock icons are now displayed as locked

   1. Note that locked icons does not mean that you typed in the authorization token correctly

1. Test the "/api/v1/fhir/Patient/$export" API

   1. Select **GET** beside the "/api/v1/fhir/Patient/$export" API

   1. Note the information about "Parameters" and "Responses"

   1. Select **Try it out**

   1. Configure the "Parameters" as follows

      - **Accept:** application/fhir+json
      
      - **Prefer:** respond-async
      
      - **_outputFormat:** application/fhir_ndjson
      
      - **_type:** ExplanationOfBenefits

   1. Select **Execute**

   1. Note the 202 response which means the export request has started

   1. Note the response

      *Example:*
      
      ```
      cache-control: no-cache, no-store, max-age=0, must-revalidate 
      connection: keep-alive 
      content-length: 0 
      content-location: http://{alb domain}/api/v1/fhir/Job/{job id}/$status 
      date: Tue, 14 Jan 2020 21:09:35 GMT 
      expires: 0 
      pragma: no-cache 
      x-content-type-options: nosniff 
      x-frame-options: DENY 
      x-xss-protection: 1; mode=block 
      ```

   1. Note the job id in the response

   1. Select **GET** again beside the "/api/v1/fhir/Patient/$export" API to collapse the section

1. Test the "/api/v1/fhir/Job/{jobUuid}/$status" API

   1. Select **GET** beside the "/api/v1/fhir/Job/{jobUuid}/$status" API

   1. Select **Try it out**

   1. Configure the "Parameters" as follows

      - **jobUuid:** {noted job id from the patient export api response}

   1. Select **Execute**
   
   1. Note the 200 response which means the job has completed

   1. Note the following attribute in the output

      *Format:*
      
      ```
      "url": "http://{alb domain}/api/v1/fhir/Job/{jobUuid}/file/{ndjson file}"
      ```

   1. Note the name of the ndjson file in the output

   1. Select **GET** again beside the "/api/v1/fhir/Job/{jobUuid}/$status" API to collapse the section

1. Test the "/api/v1/fhir/Job/{jobUuid}/file/{filename}" API

   1. Select **GET** beside the "/api/v1/fhir/Job/{jobUuid}/file/{filename}" API

   1. Select **Try it out**

   1. Configure the "Parameters" as follows

      - **Accept:** application/fhir+json

      - **filename:** {noted ndjson file}

      - **jobUuid:** {noted job id from the patient export api response}

   1. Select **Execute**

      > *** TO DO ***: determine why it isn't working

1. Verify EFS is working
   
   1. Set target AWS profile
   
      *Example for "Dev" environment:*
   
      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      ```
   
      *Example for "Sbx" environment:*
   
      ```ShellSession
      $ export AWS_PROFILE=ab2d-sbx-sandbox
      ```
   
   1. Set target environment
   
      *Example for "Dev" environment:*
   
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-dev
      ```
   
      *Example for "Sbx" environment:*
   
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      ```
   
   1. Set controller access variables
   
      *Example for "Dev" environment:*
      
      ```ShellSession
      $ export CONTROLLER_PUBLIC_IP=52.7.241.208
      $ export SSH_USER_NAME=ec2-user
      ```
   
      *Example for "Sbx" environment:*
      
      ```ShellSession
      $ export CONTROLLER_PUBLIC_IP=3.93.125.65
      $ export SSH_USER_NAME=ec2-user
      ```
   
   1. Copy the key to the controller
   
      ```ShellSession
      $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}:~/.ssh
      ```
      
   1. Connect to the controller
   
      *Format:*
      
      ```ShellSession
      $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
      ```

1. Connect to each host node and docker container

   1. Note that you will want to repeat this section for api nodes 1-2 and worker nodes 1-2

   1. Set node variables
   
      *Example for "Dev" environment (api node 1):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-dev
      $ export NODE_PRIVATE_IP=10.242.26.201
      $ export SSH_USER_NAME=ec2-user
      ```

      *Example for "Dev" environment (api node 2):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-dev
      $ export NODE_PRIVATE_IP=10.242.26.34
      $ export SSH_USER_NAME=ec2-user
      ```

      *Example for "Dev" environment (worker node 1):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-dev
      $ export NODE_PRIVATE_IP=10.242.26.4
      $ export SSH_USER_NAME=ec2-user
      ```

      *Example for "Dev" environment (worker node 2):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-dev
      $ export NODE_PRIVATE_IP=10.242.26.196
      $ export SSH_USER_NAME=ec2-user
      ```

      *Example for "Sbx" environment (api node 1):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      $ export NODE_PRIVATE_IP=10.242.31.151
      $ export SSH_USER_NAME=ec2-user
      ```
   
      *Example for "Sbx" environment (api node 2):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      $ export NODE_PRIVATE_IP=10.242.31.120
      $ export SSH_USER_NAME=ec2-user
      ```
   
      *Example for "Sbx" environment (worker node 1):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      $ export NODE_PRIVATE_IP=10.242.31.153
      $ export SSH_USER_NAME=ec2-user
      ```
   
      *Example for "Sbx" environment (worker node 2):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      $ export NODE_PRIVATE_IP=10.242.31.25
      $ export SSH_USER_NAME=ec2-user
      ```
   
   1. Connect to a node
   
      *Format:*
   
      ```ShellSession
      $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${NODE_PRIVATE_IP}
      ```

   1. Verify the ndjson file for the job under the mounted EFS on the EC2 host

      *Format:*
      
      ```ShellSession
      $ cat /mnt/efs/{job id}/{ndjson file}
      ```

   1. Connect to a running container
   
      *Example for connecting to an API container:*
   
      ```ShellSession
      $ docker exec -it $(docker ps -aqf "name=ecs-api-*" --filter "status=running") /bin/bash
      ```
   
      *Example for connecting to a worker container:*
   
      ```ShellSession
      $ docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") /bin/bash
      ```

   1. Verify the ndjson file for the job under the mounted EFS in the docker container

      *Format:*
      
      ```ShellSession
      $ cat /mnt/efs/{job id}/{ndjson file}
      ```

   1. Exit the docker container

      ```ShellSession
      $ exit
      ```

   1. Exit the EC2 host node

      ```ShellSession
      $ exit
      ```

   1. Make sure that you repeat this section for all host nodes and docker containers

## Appendix R: Update userdata for auto scaling groups through the AWS console

1. Log on to AWS

1. Select **EC2**

1. Select **Launch Configurations** under "AUTO SCALING" in the leftmost panel

1. Modify the API launch configuration

   1. Select the API launch configuration

      *Format:*

      ```
      {environment}-api-{timestamp}
      ```

   1. Select **Copy launch configuarion**

   1. Select **Configure details**

   1. Expand **Advanced Details**

   1. Modify **User data** edit box as desired

   1. Select **Skip to review**

   1. Select **Create launch configuration**

   1. Note that the following is already filled out

      *Format:*
   
      - **First dropdown:** Choose an existing key pair

      - **Select a key pair:** {environment key pair}

   1. Check **I acknowledge...**

   1. Select **Create launch configuration**

   1. Select **Close**

1. Modify the worker launch configuration

   1. Select the worker launch configuration

      *Format:*

      ```
      {environment}-worker-{timestamp}
      ```

   1. Select **Copy launch configuarion**

   1. Select **Configure details**

   1. Expand **Advanced Details**

   1. Modify **User data** edit box as desired

   1. Select **Skip to review**

   1. Select **Create launch configuration**

   1. Note that the following is already filled out

      *Format:*
   
      - **First dropdown:** Choose an existing key pair

      - **Select a key pair:** {environment key pair}

   1. Check **I acknowledge...**

   1. Select **Create launch configuration**

   1. Select **Close**

1. Select **Auto Scaling Groups** under "AUTO SCALING" in the leftmost panel

1. Modify the API auto scaling group

   1. Select the API auto scaling group

      *Format:*

      ```
      {environment}-api-{timestamp}
      ```

   1. Select **Actions**

   1. Select **Edit**

   1. Select the newly created API launch configuration with "Copy" at the end of its name from the **Launch Configuration** dropdown

      *Format:*

      ```
      {environment}-api-{timestamp}Copy
      ```

   1. Select **Save**

1. Modify the worker auto scaling group

   1. Select the worker auto scaling group

      *Format:*

      ```
      {environment}-worker-{timestamp}
      ```

   1. Select **Actions**

   1. Select **Edit**

   1. Select the newly created worker launch configuration with "Copy" at the end of its name from the **Launch Configuration** dropdown

      *Format:*

      ```
      {environment}-worker-{timestamp}Copy
      ```

   1. Select **Save**

1. Terminate the instances that were created by the old launch configuration

1. Wait for the new launch configurations to create new API and worker instances

1. Verify that the API instances received the userdata change

   1. Select **Instances** in the leftmost panel

   1. Select one of the newly created API instances

   1. Select **Actions**

   1. Select **Instance Settings**

   1. Select **View/Change User Data**

   1. Verify the userdata changes that you made are in the **User data** edit box

   1. Select **Cancel**

1. Verify that the worker instances received the userdata change

   1. Select **Instances** in the leftmost panel

   1. Select one of the newly created worker instances

   1. Select **Actions**

   1. Select **Instance Settings**

   1. Select **View/Change User Data**

   1. Verify the userdata changes that you made are in the **User data** edit box

   1. Select **Cancel**

1. Delete the original API launch configuration

   1. Select **Lauch Configurations** under the "AUTO SCALING" section in the leftmost panel

   1. Select the original API launch configuration

      *Format:*

      ```
      {environment}-api-{timestamp}
      ```

   1. Select **Actions**

   1. Select **Delete launch configuration**

   1. Select **Yes, Delete**

1. Delete the original worker launch configuration

   1. Select **Lauch Configurations** under the "AUTO SCALING" section in the leftmost panel

   1. Select the original API launch configuration

      *Format:*

      ```
      {environment}-worker-{timestamp}
      ```

   1. Select **Actions**

   1. Select **Delete launch configuration**

   1. Select **Yes, Delete**

1. Update "tfstate" file

   1. Select **S3**

   1. Navigate to automation bucket path of the changed environment

      *Example for "Dev" environment:*

      ```
      ab2d-dev-automation/ab2d-dev/terraform
      ```
      
      *Example for "Sbx" environment:*

      ```
      ab2d-sbx-sandbox-automation/ab2d-sbx-sandbox/terraform
      ```

   1. Download the "tfstate" file

   1. Note the downloaded file's name changed to the following

      ```
      terraform.json
      ```

   1. Rename the "tfstate" file in S3 to the following

      ```
      terraform.tfstate.backup
      ```

   1. Open the "terraform.json" file

      ```ShellSession
      $ vim ~/Downloads/terraform.json
      ```

   1. Change the API "launch_configuration" line as follows

      ```
      "launch_configuration": "{environment}-api-{timestamp}Copy",     
      ```

   1. Change the worker "launch_configuration" line as follows

      ```
      "launch_configuration": "{environment}-worker-{timestamp}Copy",     
      ```

   1. Save and close the file

   1. Select **S3**

   1. Navigate to automation bucket path of the changed environment

      *Example for "Dev" environment:*

      ```
      ab2d-dev-automation/ab2d-dev/terraform
      ```
      
      *Example for "Sbx" environment:*

      ```
      ab2d-sbx-sandbox-automation/ab2d-sbx-sandbox/terraform
      ```

   1. Upload the modified "terraform.json" to S3

   1. Rename "terraform.json" in S3 to the following

      ```
      terraform.tfstate
      ```