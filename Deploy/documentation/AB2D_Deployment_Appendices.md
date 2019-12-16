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
1. [Appendix H: Get log file from an API container](#appendix-h-get-log-file-from-an-api-container)
1. [Appendix I: Get log file from a worker container](#appendix-h-get-log-file-from-a-worker-container)
1. [Appendix J: Delete and recreate database](#appendix-j-delete-and-recreate-database)
1. [Appendix K: Complete DevOps linting checks](#appendix-k-complete-devops-linting-checks)
   * [Complete terraform linting](#complete-terraform-linting)
   * [Complete python linting](#complete-python-linting)
1. [Appendix L: View existing EUA job codes](#appendix-l-view-all-existing-eua-job-codes)

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

## Appendix H: Get log file from an API container

1. Set the target AWS profile

   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```
   
1. Set controller access variables

   *Example for CMS development environment:*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-shared
   $ export CONTROLLER_PUBLIC_IP=3.225.165.219
   $ export SSH_USER_NAME=ec2-user
   ```
   
1. Connect to the controller

   *Format:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
   ```

1. Set worker variables

   *Example for CMS development environment:*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-shared
   $ export API_PRIVATE_IP=10.242.26.25
   $ export SSH_USER_NAME=ec2-user
   ```
   
1. Connect to an API node

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${API_PRIVATE_IP}
   ```

1. Copy "messages" log to ec2-user home directory

   ```ShellSession
   $ sudo su
   $ cp /var/log/messages /home/ec2-user
   $ chown ec2-user:ec2-user /home/ec2-user/messages
   $ exit
   ```

1. Log off worker node

   ```ShellSession
   $ logout
   ```

1. Copy messages file to the controller

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${API_PRIVATE_IP}:~/messages .
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

## Appendix I: Get log file from a worker container

1. Set the target AWS profile

   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```
   
1. Set controller access variables

   *Example for CMS development environment:*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-shared
   $ export CONTROLLER_PUBLIC_IP=3.225.165.219
   $ export SSH_USER_NAME=ec2-user
   ```
   
1. Connect to the controller

   *Format:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
   ```

1. Set worker variables

   *Example for CMS development environment:*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-shared
   $ export WORKER_PRIVATE_IP=10.242.26.229
   $ export SSH_USER_NAME=ec2-user
   ```
   
1. Connect to a worker node

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${WORKER_PRIVATE_IP}
   ```

1. Copy "messages" log to ec2-user home directory

   ```ShellSession
   $ sudo su
   $ cp /var/log/messages /home/ec2-user
   $ chown ec2-user:ec2-user /home/ec2-user/messages
   $ exit
   ```

1. Log off worker node

   ```ShellSession
   $ logout
   ```

1. Copy messages file to the controller

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${WORKER_PRIVATE_IP}:~/messages .
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
