# Internal Test Environment Appendices

## Table of Contents

1. [Appendix A: Destroy complete environment](#appendix-a-destroy-complete-environment)
1. [Appendix B: Retest terraform using existing AMI](#appendix-b-retest-terraform-using-existing-ami)
1. [Appendix C: Stop and restart the ECS cluster](#appendix-c-stop-and-restart-the-ecs-cluster)
1. [Appendix D: Create an S3 bucket with AWS CLI](#appendix-d-create-an-s3-bucket-with-aws-cli)
1. [Appendix E: Verify EFS mounting on worker node](#appendix-e-verify-efs-mounting-on-worker-node)
1. [Appendix F: Verify PostgreSQL](#appendix-f-verify-postgresql)
1. [Appendix G: Note the product code for CentOS 7 AMI](#appendix-g-note-the-product-code-for-centos-7-ami)
1. [Appendix I: Configure the controller to test docker containers](#appendix-i-configure-the-controller-to-test-docker-containers)
1. [Appendix J: Manual create of AWS Elastic Container Registry repositories for images](#appendix-j-manual-create-of-aws-elastic-container-registry-repositories-for-images)
1. [Appendix K: Manually create an ECS cluster](#appendix-k-manually-create-an-ecs-cluster)
1. [Appendix L: Evaluate PagerDuty](#appendix-l-evaluate-pagerduty)
   * [Set up a PagerDuty free trial](#set-up-a-pagerduty-free-trial)
   * [Integrate PagerDuty with AWS CloudWatch](#integrate-pagerduty-with-aws-cloudwatch)
     * [Integrating with Global Event Rules](#integrating-with-global-event-rules)
     * [Integrating with a PagerDuty Service](#integrating-with-a-pagerduty-service)
     * [Configure AWS for the PagerDuty integration](#configure-aws-for-the-pagerduty-integration)
1. [Appendix M: Evaluate New Relic](#appendix-m-evaluate-new-relic)
   * [Set up a New Relic free trial](#set-up-a-new-relic-free-trial)
   * [Enable an integration with ECS](#enable-an-integration-with-ecs)
   * [Modify integration to include additional services](#modify-integration-to-include-additional-services)
   * [Delete the integration from New Relic](#delete-the-integration-from-new-relic)
   * [Note CMS New Relic Confluence documentation](#note-cms-new-relic-confluence-documentation)
1. [Appendix N: Evaluate Splunk](#appendix-n-evaluate-splunk)
   * [Note CMS Splunk Confluence documentation](#note-cms-splunk-confluence-documentation)
1. [Appendix O: Delete and recreate IAM instance profiles, roles, and policies](#appendix-d-delete-and-recreate-iam-instance-profiles-roles-and-policies)
   * [Delete instance profiles](#delete-instance-profiles)
   * [Delete roles](#delete-roles)
   * [Delete policies not used by IAM users](#delete-policies-not-used-by-iam-users)
1. [Appendix P: Use Amazon CloudFront to serve a static website hosted in S3](#appendix-p-use-amazon-cloudfront-to-serve-a-static-website-hosted-in-s3)
   * [Determine your starting point](#determine-your-starting-point)
   * [Request a public certificate from Certificate Manager](#request-a-public-certificate-from-certificate-manager)
   * [Generate and test the website](#generate-and-test-the-website)
   * [Create an S3 bucket for the website](#create-an-s3-bucket-for-the-website)
   * [Upload website to S3](#upload-website-to-s3)
   * [Create CloudFront distribution](#create-cloudfront-distribution)
   * [Update Route 53 DNS record to point custom CNAME to the CloudFront distribution](#update-route-53-dns-record-to-point-custom-cname-to-the-cloudfront-distribution)
   * [Test the CloudFront distribution](#test-the-cloudfront-distribution)
1. [Appendix Q: Create a VPC network peering test](#appendix-q-create-a-vpc-network-peering-test)

## Appendix A: Destroy complete environment

1. Change to the deploy directory

   *Format:*
   
   ```ShellSession
   $ cd {code directory}/ab2d/Deploy
   ```

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```
   
1. Destroy the "sbdemo-dev" environment

   *Example for Dev environment testing within SemanticBits demo environment:*
   
   ```ShellSession
   $ ./destroy-sbdemo-environment.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared
   ```

   *Example to destroy the environment, but preserve the AMIs:*
   
   ```ShellSession
   $ ./destroy-sbdemo-environment.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --keep-ami
   ```

   *Example to destroy the environment, but preserve the networking:*
   
   ```ShellSession
   $ ./destroy-sbdemo-environment.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --keep-network
   ```

   *Example to destroy the environment, but preserve both the AMIs and the networking:*
   
   ```ShellSession
   $ ./destroy-sbdemo-environment.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --keep-ami \
     --keep-network
   ```

1. Destroy the "sbdemo-sbx" environment

   *Example for Sandbox environment testing within SemanticBits demo environment:*

   ```ShellSession
   $ ./destroy-sbdemo-environment.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared
   ```

   *Example to destroy the environment, but preserve the AMIs:*
   
   ```ShellSession
   $ ./destroy-sbdemo-environment.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared \
     --keep-ami
   ```

   *Example to destroy the environment, but preserve the networking:*
   
   ```ShellSession
   $ ./destroy-sbdemo-environment.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared \
     --keep-network
   ```

   *Example to destroy the environment, but preserve both the AMIs and the networking:*
   
   ```ShellSession
   $ ./destroy-sbdemo-environment.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared \
     --keep-ami \
     --keep-network
   ```

1. Delete the VPC

   ```ShellSession
   $ ./delete-vpc-for-sbdemo.sh
   ```
   
## Appendix B: Retest terraform using existing AMI

1. If you haven't yet destroyed the existing API module, jump to the following section

   [Appendix A: Destroy complete environment](#appendix-a-destroy-complete-environment)
   
1. Set AWS profile

   ```ShellSession
   $ export AWS_PROFILE="sbdemo-dev"
   ```

1. Delete existing log file

   ```ShelSession
   $ rm -f /var/log/terraform/tf.log
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Deploy application components

   ```ShellSession
   $ ./deploy.sh --environment=sbdemo-dev --auto-approve
   ```

## Appendix C: Stop and restart the ECS cluster

1. Connect to the deployment controller instance

   *Format:*

   ```ShellSession
   $ ssh centos@{public ip address of deployment controller}
   ```
   
   *Example:*
   
   ```ShellSession
   $ ssh centos@3.84.160.10
   ```

1. Connect to the first ECS instance from the deployment controller

   *Format:*
   
   ```ShellSession
   $ ssh centos@{private ip address of first ecs instance}
   ```
   
   *Example:*
   
   ```ShellSession
   $ ssh centos@10.124.4.246
   ```

1. Stop the ecs agent on the API ECS instances

   ```ShellSession
   $ ssh centos@10.124.4.246 'docker stop ecs-agent'
   $ ssh centos@10.124.5.116 'docker stop ecs-agent'
   ```

1. Stop the ecs agent on the worker ECS instances

   ```ShellSession
   $ ssh centos@10.124.4.163 'docker stop ecs-agent'
   $ ssh centos@10.124.5.89 'docker stop ecs-agent'
   ```

1. Start the ecs agent on the API ECS instances

   ```ShellSession
   $ ssh centos@10.124.4.246 'docker start ecs-agent'
   $ ssh centos@10.124.5.116 'docker start ecs-agent'
   ```

1. Start the ecs agent on the worker ECS instances

   ```ShellSession
   $ ssh centos@10.124.4.163 'docker start ecs-agent'
   $ ssh centos@10.124.5.89 'docker start ecs-agent'
   ```

## Appendix D: Create an S3 bucket with AWS CLI

1. Create S3 file bucket

   ```ShellSession
   $ aws s3api create-bucket --bucket ab2d-cloudtrail-demo --region us-east-1
   ```

1. Note that the "Elastic Load Balancing Account ID for us-east-1" is the following:

   ```
   127311923021
   ```

1. Note that the "Elastic Load Balancing Account ID" for other regions can be found here

   > See https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/enable-access-logs.html
   
1. Block public access on bucket

   ```ShellSession
   $ aws s3api put-public-access-block \
     --bucket ab2d-cloudtrail-demo \
     --region us-east-1 \
     --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

1. Give "Write objects" and "Read bucket permissions" to the "S3 log delivery group" of the "ab2d-cloudtrail-demo" bucket

   ```ShellSession
   $ aws s3api put-bucket-acl \
     --bucket ab2d-cloudtrail-demo \
     --grant-write URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
     --grant-read-acp URI=http://acs.amazonaws.com/groups/s3/LogDelivery
   ```

1. Change to the s3 bucket policies directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/aws/s3-bucket-policies
   ```
   
1. Add this bucket policy to the "ab2d-cloudtrail-demo" S3 bucket

   ```ShellSession
   $ aws s3api put-bucket-policy \
     --bucket ab2d-cloudtrail-demo \
     --policy file://ab2d-cloudtrail-bucket-policy.json
   ```

## Appendix E: Verify EFS mounting on worker node

1. Set target profile

   *Example:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo-dev"
   ```

1. Get and note the file system id of EFS

   1. Enter the following
   
      ```ShellSession
      $ aws efs describe-file-systems | grep FileSystemId
      ```

   1. Note the output

      *Format:*
      
      ```
      "FileSystemId": "{efs file system id}",
      ```

      *Example:*
      
      ```
      "FileSystemId": "fs-2a03d9ab",
      ```

1. Connect to deployment controller

   *Format:*

   ```ShellSession
   $ ssh centos@{public ip of deployment controller}
   ```

   *Example:*
   
   ```ShellSession
   $ ssh centos@3.233.33.144
   ```
   
1. Connect to a worker node from the deployment controller

   *Format:*

   ```ShellSession
   $ ssh centos@{private ip of a worker node}
   ```
   
   *Example:*

   ```ShellSession
   $ ssh centos@10.124.4.104
   ```
   
1. Examine the file system table

   1. Enter the following
   
      ```ShellSession
      $ cat /etc/fstab
      ```

   1. Examine the EFS line in the output

      *Format:*
      
      ```
      {efs file system id} /mnt/efs efs _netdev,tls 0 0
      ```
      
      *Example:*
      
      ```
      fs-2a03d9ab /mnt/efs efs _netdev,tls 0 0
      ```

   1. Verify that the file system id matches the deployed EFS

## Appendix F: Verify PostgreSQL

1. Get database endpoint

   ```ShellSession
   $ aws rds describe-db-instances --db-instance-identifier ab2d --query="DBInstances[0].Endpoint.Address"
   ```

1. Note the output (this is the psql host)

   *Example:*
   
   ```
   ab2d.cr0bialx3sap.us-east-1.rds.amazonaws.com
   ```
   
1. Connect to the deployment controller instance

   *Format:*

   ```ShellSession
   $ ssh centos@{public ip address of deployment controller}
   ```
   
   *Example:*
   
   ```ShellSession
   $ ssh centos@52.206.57.78
   ```
   
1. Test connecting to database

   1. Enter the following
   
      *Format:*
   
      ```ShellSession
      $ psql --host {host} --username={database username} --dbname={database name}
      ```

   1. Enter database password when prompted

## Appendix G: Note the product code for CentOS 7 AMI

1. Note that the CentOS 7 AMI is used for testing in the internal test environment

1. Open Chrome

1. Enter the following in the address bar

   > https://wiki.centos.org/Cloud/AWS

1. Scroll down to the **Images** section

1. Note the product code for "CentOS-7 x86_64"

   ```
   aw0evgkw8e5c1q413zgy5pjce
   ```
   
## Appendix I: Configure the controller to test docker containers

1. Connect to the controller

   *Format:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbdemo-shared.pem centos@{controller ip address}
   ```

1. Configure AWS

   1. Configure AWS CLI

      *Example for testing Shared environment in SemanticBits demo environment:*

      ```ShellSession
      $ aws configure --profile=sbdemo-shared
      ```

   1. Enter {your aws access key} at the **AWS Access Key ID** prompt
   
   1. Enter {your aws secret access key} at the AWS Secret Access Key prompt
   
   1. Enter the following at the **Default region name** prompt
   
      ```
      us-east-1
      ```
   
   1. Enter the following at the **Default output format** prompt
   
      ```
      json
      ```
   
   1. Examine the contents of your AWS credentials file
   
      ```ShellSession
      $ cat ~/.aws/credentials
      ```

1. Install, configure, and start docker

   ```ShellSession
   $ sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
   $ sudo rpm --import https://download.docker.com/linux/centos/gpg
   $ sudo yum-config-manager --enable rhel-7-server-extras-rpms
   $ sudo yum-config-manager --enable rhui-REGION-rhel-server-extras
   $ sudo yum -y install docker-ce-18.06.1.ce-3.el7
   $ sudo usermod -aG docker centos
   $ sudo systemctl enable docker
   $ sudo systemctl start docker
   ```

1. Install Git

   ```ShellSession
   $ sudo yum install git -y
   ```

1. Clone the AB2D repo

   ```ShellSession
   $ mkdir -p ~/code
   $ cd ~/code
   $ git clone https://github.com/CMSgov/ab2d
   ```

1. Upgrade to JDK 12

   ```ShellSession
   $ cd /tmp
   $ curl -O https://download.java.net/java/GA/jdk12.0.1/69cfe15208a647278a19ef0990eea691/12/GPL/openjdk-12.0.1_linux-x64_bin.tar.gz
   $ tar -xzf openjdk-12.0.1_linux-x64_bin.tar.gz
   $ sudo mv jdk-12.0.1 /opt/
   $ cat <<EOF | sudo tee /etc/profile.d/jdk12.sh
   > export JAVA_HOME=/opt/jdk-12.0.1
   > export PATH=\$PATH:\$JAVA_HOME/bin
   > EOF
   $ source /etc/profile.d/jdk12.sh
   $ sudo rm -f /etc/alternatives/java
   $ sudo ln -s /opt/jdk-12.0.1/bin/java /etc/alternatives/java
   ```

1. Install Maven

   ```ShellSession
   $ wget https://www-us.apache.org/dist/maven/maven-3/3.6.2/binaries/apache-maven-3.6.2-bin.tar.gz -P /tmp
   $ sudo tar -xvf /tmp/apache-maven-3.6.2-bin.tar.gz -C /opt
   $ sudo ln -s /opt/apache-maven-3.6.2 /opt/maven
   $ sudo su
   $ rm -f /etc/profile.d/maven.sh
   $ echo "export M2_HOME=/opt/maven" > /etc/profile.d/maven.sh
   $ echo "export MAVEN_HOME=/opt/maven" >> /etc/profile.d/maven.sh
   $ echo "export PATH=${M2_HOME}/bin:${PATH}" >> /etc/profile.d/maven.sh
   $ exit
   $ source /etc/profile.d/maven.sh
   ```

1. Install python2 components

   ```ShellSession
   $ sudo pip install docker-compose
   $ sudo yum upgrade python* -y
   ```

1. Install python3 components

   ```ShellSession
   $ sudo yum install centos-release-scl -y
   $ sudo yum install rh-python36 -y
   $ sudo chown -R centos:centos /opt/rh/rh-python36
   $ sudo chmod -R 755 /opt/rh/rh-python36
   $ cd /opt/rh/rh-python36/root/bin
   $ ./pip3 install --upgrade pip
   $ ./pip3 install boto3
   ```

1. Set the AWS profile

   ```ShellSession
   $ export AWS_PROFILE=sbdemo-shared
   ```

1. Temporarily modify the "get-database-secret.py" file

   1. Change to the "python3" directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy/python3
      ```

   1. Open the "get-database-secret.py" file

      ```ShellSession
      $ vim get-database-secret.py
      ```

   1. Change the first line to look like this

      ```
      #!/usr/bin/env /opt/rh/rh-python36/root/bin/python3
      ```

   1. Save and close the file

1. Get database secrets

   ```ShellSession
   export CMS_ENV=sbdemo-dev
   export DATABASE_SECRET_DATETIME=2019-10-25-14-55-07
   DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
   DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
   DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
   DB_ENDPOINT=$(aws --region us-east-1 rds describe-db-instances \
     --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" \
     --output=text)
   ```

1. Build the application

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   $ rm -rf generated
   $ mkdir -p generated
   $ cp ../docker-compose.yml generated
   $ cd generated
   $ sed -i -- 's%context: ./api%context: ../../api%' docker-compose.yml
   $ sed -i -- 's%context: ./worker%context: ../../worker%' docker-compose.yml
   $ sed -i -- "s%AB2D_DB_HOST=db%AB2D_DB_HOST=$DB_ENDPOINT%" docker-compose.yml
   $ sed -i -- "s%AB2D_DB_DATABASE=ab2d%AB2D_DB_DATABASE=$DATABASE_NAME%" docker-compose.yml
   $ sed -i -- "s%AB2D_DB_USER=ab2d%AB2D_DB_USER=$DATABASE_USER%" docker-compose.yml
   $ sed -i -- "s%AB2D_DB_PASSWORD=ab2d%AB2D_DB_PASSWORD=$DATABASE_PASSWORD%" docker-compose.yml
   $ cd ../..
   $ make docker-build
   $ cd Deploy/generated
   $ docker-compose build
   ```

1. Add the following rule to "ab2d-deployment-controller-sg"

   - **Type:** Custom TCP Rule

   - **Protocol:** TCP

   - **Port Range:** 8080

   - **Source:** {lonnie's ip address}/32

   - **Description:** Whitelist Lonnie Application
   
1. Run the application using "docker-compose"

   ```ShellSession
   $ docker-compose up
   ```

1. Verify that the application is running

   *Format:*
   
   > http://{controller ip address}:8080/swagger-ui.html#/bulk-data-access-api

1. Open a second terminal

1. SSH into the controller again using the second terminal (while the application is still running in the first terminal)

   *Format:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbdemo-shared.pem centos@{controller ip address}
   ```

1. Connect to the psql shell for the RDS database instance

   ```ShellSession
   $ psql --host ab2d.cr0bialx3sap.us-east-1.rds.amazonaws.com --dbname postgres --username cmsadmin
   ```

1. List databases by entering the following in the psql shell

   ```ShellSession
   \l
   ```

1. Connect to the target database by entering the following in the psql shell

   *Format:*
   
   ```ShellSession
   \connect dev
   ```

1. Verify that the tables were created in the target database

   1. Enter the following in the psql shell

      ```ShellSession
      \dt
      ```

   1. Note the tables in the output
    
      *Example:*
      
       Schema |         Name          | Type  | Owner 
      --------|-----------------------|-------|-------
       public | beneficiary           | table | ab2d
       public | contract              | table | ab2d
       public | coverage              | table | ab2d
       public | databasechangelog     | table | ab2d
       public | databasechangeloglock | table | ab2d
       public | int_channel_message   | table | ab2d
       public | int_group_to_message  | table | ab2d
       public | int_lock              | table | ab2d
       public | int_message           | table | ab2d
       public | int_message_group     | table | ab2d
       public | int_metadata_store    | table | ab2d
       public | job                   | table | ab2d
       public | job_output            | table | ab2d
       public | role                  | table | ab2d
       public | sponsor               | table | ab2d
       public | user_account          | table | ab2d
       public | user_role             | table | ab2d

   1. Exist the psql shell

      ```ShellSession
      \q
      ```
      
1. List the running containers

   1. Enter the following

      ```ShellSession
      $ docker ps
      ```

   1. Note the output

      *Example showing only the key columns:*
      
      CONTAINER ID|IMAGE           |PORTS                 |NAMES
      ------------|----------------|----------------------|---------------
      85e5595b9e64|generated_api   |0.0.0.0:8080->8080/tcp|generated_api_1
      01215a162bc2|generated_worker|                      |generated_worker_1
      0ec7d4563567|postgres:11     |0.0.0.0:5432->5432/tcp|generated_db_1

1. Examine the running api container

   1. Connect to the running api container
   
      ```ShellSession
      $ docker exec -it $(docker ps -aqf "name=generated_api_1") /bin/bash
      ```

   1. Verify the database host environmental setting

      ```ShellSession
      $ echo $AB2D_DB_HOST
      ```

   1. Verify the database user environment setting

      ```ShellSession
      $ echo $AB2D_DB_USER
      ```

   1. Verify the database password environment setting

      ```ShellSession
      $ echo $AB2D_DB_PASSWORD
      ```

   1. Verify the database name environment setting

      ```ShellSession
      $ echo $AB2D_DB_DATABASE
      ```

   1. Change to the application directory

      ```ShellSession
      $ cd /usr/src/ab2d-api
      ```
      
   1. Exit the container

      ```ShellSession
      exit
      ```

1. Examine the running worker container

   1. Connect to the running worker container

      ```ShellSession
      $ docker exec -it $(docker ps -aqf "name=^ab2d_worker.*$") /bin/bash
      ```

   1. Exit the container

      ```ShellSession
      exit
      ```

1. Delete all the containers

   1. Delete orphaned volumes (if any)

      ```ShellSession
      $ docker volume ls -qf dangling=true | xargs -I name docker volume rm name
      ```

   1. Delete all containers (if any)
      
      ```ShellSession
      $ docker ps -aq | xargs -I name docker rm --force name
      ```

   1. Delete orphaned volumes again (if any)

      ```ShellSession
      $ docker volume ls -qf dangling=true | xargs -I name docker volume rm name
      ```

1. Test an nginx container

   1. Pull the nginx container from docker hub

      ```ShellSession
      $ docker pull nginx
      ```

   1. Run the nginx container with "docker run"

      ```ShellSession
      $ docker run -d -p 8080:80 nginx:latest
      ```

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*
      
      > http://{controller ip address}:8080

   1. Verify that the "Welcome to nginx!" page is displayed

1. Modify the API dockerfile so that it can be run with "docker run" instead of "docker-compose"

   > *** TO DO ***

1. Change to the docker file's directory

   ```ShelLSession
   $ cd ~/code/ab2d/Deploy/generated
   ```
   
1. Create the docker image

   *Format:*
   
   ```ShellSession
   $ docker build \
     --build-arg ab2d_db_host_arg={database instance host} \
     --build-arg ab2d_db_port_arg={database instance port} \
     --build-arg ab2d_db_database_arg={database instance db} \
     --build-arg ab2d_db_user_arg={database instance user} \
     --build-arg ab2d_db_password_arg={database instance passwordb} \
     --tag generated_api:latest .
   ```

1. Verify that the docker image was created

   1. Enter the following

      ```ShellSession
      $ docker images | grep generated_api
      ```

   1. Verify that the following appears in the output

      *Example:*
      
      ```
      generated_api       latest              04e90c9d1d9f        About a minute ago   555MB
      ```

1. Run the API container with "docker run"

   ```ShellSession
   $ docker run -d --name ab2d_api -p 8080:8080 generated_api:latest
   ```

1. Determine if the container is running

   1. Wait 30 seconds
   
   1. Enter the following
   
      ```ShellSession
      $ netstat -an | grep 8080
      ```

   1. If there is no output, the container is not running

1. If the container is not running, do the following

   1. View the stopped container
   
      ```Shell
      $ docker ps -a | grep ab2d_api
      ```

   1. View the log of the stopped container

      ```ShellSession
      $ docker logs -t ab2d_api
      ```

1. If the container is running, do the following

   1. Open Chrome

   2. Enter the following in the address bar

      *Example:*
      
      > http://34.203.8.2:8080/swagger-ui.html#/bulk-data-access-api

   3. Verify that the "AB2D FHIR Bulk Data Access API" page is displayed

## Appendix J: Manual create of AWS Elastic Container Registry repositories for images

1. Set target profile

   *Example:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo-dev"
   ```

1. Authenticate Docker to default Registry
   
   ```ShellSession
   $ read -sra cmd < <(aws ecr get-login --no-include-email)
   $ pass="${cmd[5]}"
   $ unset cmd[4] cmd[5]
   $ "${cmd[@]}" --password-stdin <<< "$pass"
   ```

1. Note that the authentication is good for a 12 hour session

1. If you want to delete all images and containers in your local environment, do the following:
    
   1. Delete orphaned volumes (if any)

      ```ShellSession
      $ docker volume ls -qf dangling=true | xargs -I name docker volume rm name
      ```

   1. Delete all containers (if any)
      
      ```ShellSession
      $ docker ps -aq | xargs -I name docker rm --force name
      ```

   1. Delete all images (if any)

      ```ShellSession
      $ docker images -q | xargs -I name docker rmi --force name
      ```

   1. Delete orphaned volumes again (if any)

      ```ShellSession
      $ docker volume ls -qf dangling=true | xargs -I name docker volume rm name
      ```

   1. Delete all images again (if any)

      ```ShellSession
      $ docker images -q | xargs -I name docker rmi --force name
      ```

1. Change to the repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Build the docker images of API and Worker nodes

   1. Build all docker images

      ```ShellSession
      $ make docker-build
      ```

   1. Check the docker images

      ```ShellSession
      $ docker image ls
      ```

   1. Note the output only includes the following

      *Format:*

      ```
      {repository}:{tag}
      ```

      *Example:*

      ```
      maven:3-jdk-12
      ```
      
   1. Build with "docker-compose"

      ```ShellSession
      $ docker-compose build
      ```
      
   1. Check the docker images

      ```ShellSession
      $ docker image ls
      ```

   1. Note the output includes the following

      - ab2d_worker:latest

      - ab2d_api:latest

      - maven:3-jdk-12

      - openjdk:12

1. If you want to run the containers, do the following

   1. Enter the following
   
      ```ShellSession
      $ docker-compose up
      ```

   1. Wait for the following to appear in the output

      ```
      ab2d_build_1 exited with code 0
      ```

1. If you want to verify that the application is running correctly, do the following

   1. Open Chrome

   2. Enter the following in the address bar

      > http://localhost:8080/swagger-ui.html#/bulk-data-access-api

   3. Verify that the "AB2D FHIR Bulk Data Access API" page is displayed

1. If you want to connect to the running db container, do the following

   1. Connect to the db container
   
      ```ShellSession
      $ docker exec -it $(docker ps -aqf "name=^ab2d_db.*$") /bin/bash
      ```

   1. Launch the psql shell

      *Format:*
      
      ```ShellSession
      $ psql --username=ab2d
      ```

   1. List databases

      ```ShellSession
      \l
      ```

   1. Note the database in the output

      Name|Owner|Encoding|Collate   |Ctype
      ----|-----|--------|----------|----------
      ab2d|ab2d |UTF8    |en_US.utf8|en_US.utf8

   1. List tables
   
      ```ShellSession
      \dt
      ```

   1. Note the tables in the output

      *Example:*
      
       Schema |         Name          | Type  | Owner 
      --------|-----------------------|-------|-------
       public | beneficiary           | table | ab2d
       public | contract              | table | ab2d
       public | coverage              | table | ab2d
       public | databasechangelog     | table | ab2d
       public | databasechangeloglock | table | ab2d
       public | int_channel_message   | table | ab2d
       public | int_group_to_message  | table | ab2d
       public | int_lock              | table | ab2d
       public | int_message           | table | ab2d
       public | int_message_group     | table | ab2d
       public | int_metadata_store    | table | ab2d
       public | job                   | table | ab2d
       public | job_output            | table | ab2d
       public | role                  | table | ab2d
       public | sponsor               | table | ab2d
       public | user_account          | table | ab2d
       public | user_role             | table | ab2d

   1. Quit the psql shell

      ```ShellSession
      \q
      ```

   1. Exit the container

      ```ShellSession
      exit
      ```
      
1. If you want to connect to the running api container, do the following

   1. Connect to the api container
   
      ```ShellSession
      $ docker exec -it $(docker ps -aqf "name=^ab2d_api.*$") /bin/bash
      ```

   1. Exit the container

      ```ShellSession
      exit
      ```

1. If you want to connect to the running worker container, do the following

   1. Connect to the worker container

      ```ShellSession
      $ docker exec -it $(docker ps -aqf "name=^ab2d_worker.*$") /bin/bash
      ```

   1. Exit the container

      ```ShellSession
      exit
      ```

1. Create an AWS Elastic Container Registry (ECR) for "ab2d_api"

   ```ShellSession
   $ aws ecr create-repository --repository-name ab2d_api
   ```

1. Tag the "ab2d_api" image for ECR

   *Format:*
   
   ```ShellSession
   $ docker tag ab2d_api:latest {aws account number}.dkr.ecr.us-east-1.amazonaws.com/ab2d_api:latest
   ```

   *Example:*
   
   ```ShellSession
   $ docker tag ab2d_api:latest 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_api:latest
   ```

1. Push the "ab2d_api" image to ECR

   *Format:*
   
   ```ShellSession
   $ docker push {aws account number}.dkr.ecr.us-east-1.amazonaws.com/ab2d_api:latest
   ```

   *Example:*
   
   ```ShellSession
   $ docker push 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_api:latest
   ```

1. Create an AWS Elastic Container Registry (ECR) for "ab2d_worker"

   ```ShellSession
   $ aws ecr create-repository --repository-name ab2d_worker
   ```

1. Tag the "ab2d_worker" image for ECR

   *Format:*
   
   ```ShellSession
   $ docker tag ab2d_worker:latest {aws account number}.dkr.ecr.us-east-1.amazonaws.com/ab2d_worker:latest
   ```

   *Example:*
   
   ```ShellSession
   $ docker tag ab2d_worker:latest 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_worker:latest
   ```

1. Push the "ab2d_worker" image to ECR

   *Format:*
   
   ```ShellSession
   $ docker push {aws account number}.dkr.ecr.us-east-1.amazonaws.com/ab2d_worker:latest
   ```

   *Example:*
   
   ```ShellSession
   $ docker push 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_worker:latest
   ```

## Appendix K: Manually create an ECS cluster

1. Set the AWS profile

   ```ShellSession
   $ export AWS_PROFILE=sbdemo-shared
   ```
      
1. Create the ECS cluster using ECS CLI

   ```ShellSession
   $ ecs-cli up \
     --cluster ab2d-dev-api-test \
     --instance-role Ab2dInstanceRole \
     --keypair ab2d-sbdemo-shared \
     --size 2 \
     --azs "us-east-1a, us-east-1b" \
     --port 80 \
     --instance-type m5.xlarge \
     --image-id ami-07836bb8608f81187 \
     --launch-type EC2 \
     --region us-east-1
   ```

1. Change to the shared environment

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-sbdemo-shared
   ```

1. Add the following rule to the security group associated with the first ecs instance

   - **Type:** SSH

   - **Protocol:** TCP

   - **Port Range:** 22

   - **Source:** Custom 152.208.13.223/32

1. Select **Save**

1. Configure ECS instances

   1. Connect to an ECS instance
   
      *Format:*
   
      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-sbdemo-shared.pem centos@{public ip of first ecs instance}
      ```
   
      *Example for a first ECS instance:*
   
      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-sbdemo-shared.pem centos@3.92.52.181
      ```

      *Example for a second ECS instance:*
   
      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-sbdemo-shared.pem centos@3.86.202.84
      ```

   1. Set more useful hostname
   
      ```ShellSession
      $ export env=dev
      $ echo "$(hostname -s).ab2d-${env}" > /tmp/hostname
      $ sudo mv /tmp/hostname /etc/hostname
      $ sudo hostname "$(hostname -s).ab2d-${env}"
      ```
   
   1. Create ECS config file
   
      *IMPORTANT: Don't forget to change the ECS_CLUSTER value below with the cluster name.*
      
      ```ShellSession
      $ sudo mkdir -p /etc/ecs
      $ sudo sh -c 'echo "
        ECS_DATADIR=/data
        ECS_ENABLE_TASK_IAM_ROLE=true
        ECS_ENABLE_TASK_IAM_ROLE_NETWORK_HOST=true
        ECS_LOGFILE=/log/ecs-agent.log
        ECS_AVAILABLE_LOGGING_DRIVERS=[\"json-file\",\"awslogs\",\"syslog\",\"none\"]
        ECS_ENABLE_TASK_IAM_ROLE=true
        ECS_UPDATE_DOWNLOAD_DIR=/var/cache/ecs
        SSL_CERT_DIR=/etc/pki/tls/certs
        ECS_ENABLE_AWSLOGS_EXECUTIONROLE_OVERRIDE=true
        ECS_UPDATES_ENABLED=true
        ECS_ENABLE_TASK_IAM_ROLE_NETWORK_HOST=true
        ECS_LOGFILE=/log/ecs-agent.log
        ECS_CLUSTER="ab2d-dev-api-test"
        ECS_LOGLEVEL=info" > /etc/ecs/ecs.config'
      ```
   
   1. Autostart the ecs client
   
      ```ShellSession
      $ sudo docker run --name ecs-agent \
        --detach=true \
        --restart=on-failure:10 \
        --volume=/var/run:/var/run \
        --volume=/var/log/ecs/:/log \
        --volume=/var/lib/ecs/data:/data \
        --volume=/etc/ecs:/etc/ecs \
        --net=host \
        --env-file=/etc/ecs/ecs.config \
        --privileged \
        --env-file=/etc/ecs/ecs.config \
        amazon/amazon-ecs-agent:latest
      ```
   
   1. Log off the instance
   
      ```ShellSession
      $ exit
      ```
   
   1. Repeat this section for any other ECS instances
   

1. Create an application load balanacer

   1. Select **EC2**

   1. Select **Load Balanacers** in the leftmost panel

   1. Select **Create Load Balancer**

   1. Select **Create** under *Application Load Balancer*

   1. Configure the "Basic Configuration" as follows

      - **Name:** ab2d-dev-api-test-alb

      - **Scheme:** internet-facing

      - **IP address type:** ipv4

   1. Configure "Listeners" as follows

      - **Load Balancer Protocol:** HTTP

      - **Load Balancer Port:** 80

   1. Configure "Availability Zones" as follows

      - **VPC:** {select the vpc where the ecs instances reside}

      - **Availability Zones:**

        - us-east-1a {checked with public subnet selected}

        - us-east-1b {checked with public subnet selected}

   1. Select **Next: Configure Security Settings**

1. Select **Next: Configure Security Groups**

1. Configure security groups

   1. Select **Select an existing security group**

   1. Select the security group associated with the ECS isntances

   1. Select **Next: Configure Routing**

1. Configure routing as follows

   1. Configure "Target group"
   
      - **Target group:** New target group

      - **Name:** ab2d-dev-api-test-tg

      - **Target type:** Instance

      - **Protocol:** HTTP

      - **Port:** 80

   1. Configure "Health checks"

      - **Protocol:** HTTP

      - **Path:** /

    1. Select **Next: Register Targets**

1. Select the ECS instances that participate in the cluster

1. Select **Add to registered**

1. Select **Next: Review**

1. Select **Create**

1. Select **Close**
   
1. Configure ECS

   1. Select the ECS cluster

      *Example:*

      ```
      ab2d-dev-api-test
      ```

   1. Select the **Services** tab

   1. Select **Create**

   1. Configure the service as follows

      - **Launch type:** EC2

      - **Task Definition:** api

      - **Revision:** {number} (latest)

      - **Cluster:** ab2d-dev-api-test

      - **Service name:** api

      - **Service type:** DAEMON

      - **Number of tasks:** Automatic

      - **Minimum healthy percent:** 0

      - **Maximum percent:** 100

   1. Keep remaining defaults

   1. Select **Next step**

   1. Configure "Load balancing" under "Configure network"

      - **Load balancer type:** Application Load Balancer

      - **Service IAM Role:** AWSServiceRoleForECS

      - **Load balancer name:** ab2d-dev-api-test-alb

   1. Configure "Container to load balance"

      - **Container name : port:** ab2d-api:80:8080

   1. Select **Add to load balanacer**

   1. Configure "ad2d-api : 8080"

      - **Production listener port:** 80:HTTP

      - **Production listener protocol:** HTTP

      - **Target group name:** create-new ab2d-dev-api-test-ecs-tg

      - **Target group protocol:** HTTP

      - **Target type:** instance

      - **Pattern:** /\*

      - **Evaluation order:** 1

      - **Health check path:** /

   1. Keep remaining defaults

   1. Select **Next step** on the *Configure network* page

   1. Select **Next step** on the *Set Auto Scaling* page

   1. Select **Create Service**

1. Select **View Service**

## Appendix L: Evaluate PagerDuty

### Set up a PagerDuty free trial

1. Open Chrome

1. Enter the following in the address bar

   > https://www.pagerduty.com/

1. Select **TRY NOW**

1. Enter the following on the "14 Day free trial" page

   *Format:
   
   - **First Name:** {your first name}

   - **Last Name:** {your last name)

   - **Email:** {your semanticbits email)

   - **Password:** {your desired "alphanumeric only" password}

   - **Organization Name:** SemanticBits

   - **Subdomain:** https://{lowercase first name}-{lowercase last name}.pagerduty.com

1. Select **GET STARTED**
   
### Integrate PagerDuty with AWS CloudWatch

1. Note PagerDuty's "Onboarding Goal #1"

   ```
   ONBOARDING GOAL #1

   Finish Setting Up Your First Integration
   
   Let's finish your first integration! Once you click the service name below you will
   be redirected to a service detail page.
   
   Click on the integrations tab first, you'll need your 'Integration Key' so make sure
   to copy it.
   
   Once you have the Integration Key copied you can see a step-by-step walk through by
   clicking on the integration type to the right.
   
   Once you've configured this integration, your data should then be flowing into PagerDuty.
   You can add multiple integrations to a single service. We recommend grouping integrations.
   This allows you to better represent the actual entities you are monitoring, managing, and
   operating.
   ```

1. Log on to your PagerDuty account

   1. Open Chrome

   1. Enter the following in the address bar

      > https://{lowercase first name}-{lowercase last name}.pagerduty.com

   1. Enter the following
   
      - **Email:** {your semanticbits email)
   
      - **Password:** {your desired password}

   1. Select **Sign In**

1. View PagerDuty integration guide for CloudWatch
   
   1. Select the ![Apps](images/pager-duty-apps-icon.png) icon
   
   1. Select **Integrations Directory**
   
   1. Note that a new Chrome tab will automatically open
   
   1. Scroll down to the "Intergration Library" section
   
   1. Select **Amazon CloudWatch**
   
   1. Note that the following page will open
   
      > https://support.pagerduty.com/docs/aws-cloudwatch-integration-guide

1. Note there are two ways to integrate CloudWatch with PagerDuty

   - Integrating with Global Event Rules

   - Integrating with a PagerDuty Service

1. If you need to route alerts to different reponders based on the payload, jump to the following section:

   [Integrating with Global Event Rules](#integrating-with-global-event-rules)

1. If you don't need to route alerts to different reponders based on the payload, jump to the following section:

   [Integrating with a PagerDuty Service](#integrating-with-a-pagerduty-service)

#### Integrating with Global Event Rules

1. Log on to your PagerDuty account

   1. Open Chrome

   1. Enter the following in the address bar

      > https://{lowercase first name}-{lowercase last name}.pagerduty.com

   1. Enter the following
   
      - **Email:** {your semanticbits email)
   
      - **Password:** {your desired password}

   1. Select **Sign In**

1. Create "CloudWatch Dev" Global Event Rules

   1. Select the **Configuration** menu

   1. Select **Event Rules**

   1. Note the following information

      - **Integration Key:** {integration key}

   1. Note that you will use the following "Integration URL" when configuring AWS

      ```
      https://events.pagerduty.com/x-ere/{integration key}
      ```

1. Jump to the following section

   [Configure AWS for the PagerDuty integration](#configure-aws-for-the-pagerduty-integration)

#### Integrating with a PagerDuty Service

1. Log on to your PagerDuty account

   1. Open Chrome

   1. Enter the following in the address bar

      > https://{lowercase first name}-{lowercase last name}.pagerduty.com

   1. Enter the following
   
      - **Email:** {your semanticbits email)
   
      - **Password:** {your desired password}

   1. Select **Sign In**

1. Create a "CloudWatch Dev" PagerDuty Service

   1. Select the **Configuration** menu
   
   1. Select **Services**
   
   1. Select **New Service**
   
   1. Configure "General Settings" as follows
   
      - **Name:** CloudWatch Dev

   1. Configure "Integration Settings"
   
      - **Intergration Type:** Amazon CloudWatch
   
      - **Intergration Name:** Amazon CloudWatch
   
   1. Note the following about the integration
   
      ```
      Amazon Web Services CloudWatch provides monitoring for AWS cloud resources and
      customer-run applications.
   
      This integration supports alarms from CloudSearch, DynamoDB, EBS, EC2, ECS,
      ElastiCache, ELB, ES, Kinesis, Lambda, ML, Redshift, RDS, Route53, SNS, SQS, S3,
      SWF, StorageGateway
      ```
   
   1. Configure "Incident Settings"
   
      - **Escalation Policy:** Default
   
   1. Select **Create alerts and incidents** under "Incident Behavior"
   
   1. Select **Intelligently based on the alert content and past groups** under "Alert Grouping"
   
   1. Select **Add Service**
   
   1. Note the following information
   
      - **Type:** Amazon CloudWatch
   
      - **Integration Key:** {integration key}
   
      - **Integration URL:** https://events.pagerduty.com/integration/{integration key}/enqueue
   
      - **Correlate events by:** Alarm Name
   
      - **Derive name from:** Default
   
   1. Copy and save the **Integration URL**

   1. Note that you will use the following "Integration URL" when configuring AWS

      ```
      https://events.pagerduty.com/integration/{integration key}/enqueue
      ```

1. Jump to the following section

   [Configure AWS for the PagerDuty integration](#configure-aws-for-the-pagerduty-integration)

#### Configure AWS for the PagerDuty integration

1. Open Chrome

1. Log on to the AWS console

1. Select Simple Notification Service (SNS)

1. Select **Topics** in the leftmost panel

1. Select **Create topic**

1. Configure the "Create topic" page as follows

   - **Name:** ab2d-dev-cloudwatch

   - **Dispaly Name:** ab2d-dev-cloudwatch

1. Select **Create topic**

1. Select **Subscriptions** in the leftmost panel

1. Select **Create Subscription**

1. Configure "Details" as follows

   *Format:*
   
   - **Topic ARN:** arn:aws:sns:us-east-1:{aws account number}:ab2d-dev-cloudwatch

   - **Protocol:** HTTPS

   - **Endpoint:** https://events.pagerduty.com/integration/{encryption key}/enqueue

   - **Enable raw message delivery:** unchecked

   *Example:*
   
   - **Topic ARN:** arn:aws:sns:us-east-1:114601554524:ab2d-dev-cloudwatch

   - **Protocol:** HTTPS

   - **Endpoint:** https://events.pagerduty.com/integration/73fc28b6128a46999d90d6d119f9e8e5/enqueue

   - **Enable raw message delivery:** unchecked

1. Select **Create subscription**

1. Note that the status dispays "Pending confirmation"

1. Refresh the page

1. Note that the status should now display "Confirmed"

1. Select the **EC2** service

1. Select one of the API EC2 instances

1. Select **Actions**

1. Select **CloudWatch Monitoring**

1. Select **Add/Edit Alarms**

1. Select **Create Alarm**

1. Check **Send a notification to**

1. Select the topic that you created from the **Send a notification to** dropdown

   ```
   ab2d-dev-cloudwatch
   ```

1. Configure the alarm as follows

   - **Whenever:** Average of CPU Utilization

   - **Is:** >= 90 Percent

   - **For at least:** 2 consecutive period(s) of 5 minute(s)

   - **Name of alarm:** ab2d-dev-api-cpu-alarm

1. Select **Create Alarm**

1. Select **view** under "More Options"

1. Select **Edit**

1. Select **Next** on the bottom of the page

1. Note that if desired you can add additional notifications by select **Add notofication**

1. If you made any changes, select **Update alarm**; otherwise, select **Cancel**

## Appendix M: Evaluate New Relic

### Set up a New Relic free trial

1. Open Chrome

1. Enter the following in the address bar

   > https://newrelic.com

1. Select **Sign Up**

1. Enter the following information

   - **First Name:** {your first name}

   - **Last Name:** {your last name}

   - **Your Business Email Address:** {your semanticbits email}

   - **Retype Your Business Email Address:** {your semanticbits email}

   - **Your Phone Number:** {your phone number}

   - **Select Your Country:** {your country}

   - **Select Your State:** {your state (if displayed)}

   - **What's Your Postal Code:** {your postal code (if displayed)}

   - **Where do you want your data house:** {United States|European Union}

   - **Your company:** SemanticBits

   - **Your role:** {your role}

   - **Number of Employees:** 101-1,000

   - **Number of App Servers:** 3-10 Servers

   - **I'm not a robot:** checked

   - **I agree to the Terms and Service:** checked

1. Select **Sign Up for New Relic**

1. Wait for email from New Relic

1. Select **Verify your email** in the email from New Relic

1. Set your password

   - **Password:** {your password}

   - **Password confirmation:** {your password}

1. Select **Update**

1. Log into New Relic

   - **Email:** {your semanticbits email}

   - **Password:** {your password}

1. Select **Sign in**

1. Select **New Relic Infrastructure**

1. Select **Start my 30 day free trial**

### Enable an integration with ECS

1. Log on to New Relic

1. Select the **Infrastructure** tab

1. Select the **AWS** tab

1. Select **Amazon Web Services**

1. Note the AWS services that are included with a "New Relic Infrastructure" subscription

   - API Gateway

   - AppSync

   - Athena

   - AutoScaling

   - Billing

   - CloudFront

   - CloudTrail

   - Direct Connect

   - DocumentDB

   - DynamoDB

   - EBS

   - EC2

   - ECS

   - EFS

   - Elastic Beanstalk

   - ElastiCache

   - Elasticsearch Service

   - ELB

   - ELB (Classic)

   - EMR

   - Glue

   - Health

   - IAM

   - IoT

   - Kinesis Firehose

   - Kineses Streams

   - Lambda

   - Managed Kafka

   - MQ

   - QLDB

   - RDS

   - Redshift

   - Route 53

   - S3

   - SES

   - SNS

   - SQS

   - Step Functions

   - Trusted Advisor

   - VPC

   - WAF

1. Select **ECS**

1. Create a role and establish trust

   1. Note the  New Relic provided AWS account id

   1. Log on to AWS

   1. Select IAM

   1. Select **Roles** in the leftmost panel

   1. Select **Create role**

   1. Select **Another AWS account**

   1. Enter the New Relic provided "Account ID" in the **Account ID** text box

   1. Check **Require external ID**

   1. Enter the New Relic provided "External ID" in the **Account ID** text box

   1. Select **Next: Permissions**

   1. Return to New Relic

   1. Select **Next** on the "Step 1: Create a role and establish trust" page

 1. Attach policy

    1. Return to AWS

    1. Type the following in the **Search** text box on the "Attach permissions policies" page

       ```
       ReadOnlyAccess
       ```

    1. Check **ReadOnlyAccess** from the search results

    1. Select **Next: Tags**

    1. Select **Next: Review**

    1. Return to New Relic

    1. Select **Next** on the "Step 2: Attach policy" page

 1. Set role name and review

    1. Return to AWS

    1. Type the following in the **Role name** text box

       ```
       Ab2dNewRelicInfrastructureIntegrations
       ```

    1. Select **Create role**

    1. Select **Roles** in the leftmost panel

    1. Find and select the newly created role

       ```
       Ab2dNewRelicInfrastructureIntegrations
       ```

    1. Copy the ARN for the role

       *Format:*
       
       ```
       arn:aws:iam::{your aws account}:role/Ab2dNewRelicInfrastructureIntegrations
       ```

    1. Return to New Relic

    1. Select **Next** on the "Step 3: Set role name and review" page

1. Select **Next** on the "Step 4: Configure Budgets policy" page

1. Enter the following on the "Step 5: Add Account Details" page

   *Example for sbdemo:*
   
   - **AWS Account Name:** semanticbitsdemo

   - **ARN:** arn:aws:iam::{your aws account}:role/Ab2dNewRelicInfrastructureIntegrations

1. Select **Next** on the "Step 5: Add Account Details" page

1. Uncheck **Select all**

1. Check **ECS**

1. Select **Next** on the "Step 6: Select Services" page

1. Note the following message is displayed

   ```
   We're setting up your integration

   New Relic is retrieving monitoring data from AWS account semanticbitsdemo and
   configuring AWS dashboards in New Relic Insights. This may take up to 5 minutes.
   ```

1. Select **OK** on the "We're setting up your integration" page

1. Select **ECS dashboard**

### Modify integration to include additional services

1. Log on to New Relic

1. Select the **Infrastructure** tab

1. Select the **AWS** tab

1. Note the current services that are displayed

   - ECS

1. Select **Manage Services**

1. Check additional desired services that are deployed

   *Example:*

   - AutoScaling
   
   - CloudTrail

   - EBS
   
   - EC2

   - EFS

   - ELB

   - IAM

   - RDS

   - S3

   - VPC

   - WAF

1. Select **Save...**

1. Select the **AWS** tab

1. Note that each service now has a row with dashboards, etc.

1. Configure a service to display information about only one region

   1. Note that as an example whe will configure VPC

   1. Select **Configure** within the "VPC" row

   1. Switch **Limit to AWS regions** to "ON"

   1. Check **us-east-1**

   1. Select **Submit**

### Delete the integration from New Relic

1. Log on to New Relic

1. Select the **Infrastructure** tab

1. Select the **AWS** tab

1. Select **Manage Services**

1. Select **Unlink this account**

1. Select **Unlink semanticbitsdemo**

## Note CMS New Relic Confluence documentation

1. Note the "New Relic Home" CMS Confluence page

   > https://confluence.cms.gov/display/NR/New+Relic+Home

1. Note the "Need a New Relic account" CMS Confluence page

   > https://confluence.cms.gov/pages/viewpage.action?pageId=124456202

## Appendix N: Evaluate Splunk

### Note CMS Splunk Confluence documentation

1. Note the "Splunk Onboarding" CMS Confluence page

   > https://confluence.cms.gov/display/MCM2/Splunk+Onboarding

1. Note the "Splunk Knowledge Base" CMS Confluence page

   > https://confluence.cms.gov/display/CETS/Splunk+Knowledge+Base

1. Note the "CMS Splunk Topology" CMS Confluence page

   > https://confluence.cms.gov/display/CETS/CMS+Splunk+Topology

## Appendix O: Delete and recreate IAM instance profiles, roles, and policies

### Delete instance profiles

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo-shared"
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
   $ export AWS_PROFILE="sbdemo-shared"
   ```

1. Detach policies from the "Ab2dManagedRole" role

   ```ShellSession
   $ aws iam detach-role-policy \
     --role-name Ab2dManagedRole \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dAccessPolicy
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
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dAssumePolicy
   $ aws iam detach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dPackerPolicy
   $ aws iam detach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dS3AccessPolicy
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
   $ export AWS_PROFILE="sbdemo-shared"
   ```

1. Delete "Ab2dAccessPolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dAccessPolicy
   ```

1. Delete "Ab2dAssumePolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dAssumePolicy
   ```

1. Delete "Ab2dPackerPolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dPackerPolicy
   ```

1. Delete "Ab2dS3AccessPolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dS3AccessPolicy
   ```

## Appendix P: Use Amazon CloudFront to serve a static website hosted in S3

### Determine your starting point

1. Note that CMS manages their own Route 53 and provides the required domains and keys

1. If you have access to a domain that you can personally administer in Route 53 and want to do a test setup, jump to the following section:

   [Request a public certificate from Certificate Manager](#request-a-public-certificate-from-certificate-manager)

1. If you just want to test setting up an S3 website with a CloudFront distribution, jump to the following section:

   [Generate and test the website](#generate-and-test-the-website)
   
### Request a public certificate from Certificate Manager

1. If you already have a wildcard certificate, jump to the following section:

   [Generate and test the website](#generate-and-test-the-website)

1. Set the target AWS profile

   *Format:*
   
   ```ShellSession
   $ export AWS_PROFILE={target aws profile}
   ```
   
1. Create a certificate for a specific subdomain

   *Format:*
   
   ```ShellSession
   $ aws --region us-east-1 acm request-certificate \
     --domain-name "*.{domain}" \
     --subject-alternative-names "{domain}" \
     --validation-method DNS \
     --idempotency-token 1234 \
     --options CertificateTransparencyLoggingPreference=ENABLED
   ```

   *Example:*
   
   ```ShellSession
   $ aws --region us-east-1 acm request-certificate \
     --domain-name "*.example.com" \
     --subject-alternative-names "example.com" \
     --validation-method DNS \
     --idempotency-token 1234 \
     --options CertificateTransparencyLoggingPreference=ENABLED
   ```

1. Note that if "CertificateTransparencyLoggingPreference" is not enabled, Google Chrome will not display the web page

1. Open Chrome

1. Log on to AWS

1. Navigate to Certificate Manager

1. Expand the row for the requested cerificate

   *Format:*
   
   ```
   *.{domain}
   ```

   *Example:*

   ```
   *.example.com
   ```

1. Validate the wildcard domain

   1. Expand the wildcard domain under the "Domain" section
   
      *Format:*
      
      ```
      *.{domain}
      ```
   
      *Example:*
   
      ```
      *.example.com
      ```
   
   1. Select **Create record in Route 53**
   
   1. Select **Create**
   
   1. Note that a message like the following should appear
   
      ```
      Success
      The DNS record was written to your Route 53 hosted zone. It can take 30 minutes or longer
      for the changed to propagate and for AWS to validate the domain and issue the certificate.
      ```

1. Validate the root domain

   1. Expand the wildcard domain under the "Domain" section
   
      *Format:*
      
      ```
      {domain}
      ```
   
      *Example:*
   
      ```
      example.com
      ```
   
   1. Select **Create record in Route 53**
   
   1. Select **Create**
   
   1. Note that a message like the following should appear
   
      ```
      Success
      The DNS record was written to your Route 53 hosted zone. It can take 30 minutes or longer
      for the changed to propagate and for AWS to validate the domain and issue the certificate.
      ```

1. Wait for an hour or so

1. Open Chrome

1. Log on to AWS

1. Navigate to Certificate Manager

1. Verify that the "Status" displays the following

   ```
   Issued
   ```

### Generate and test the website

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
    
   1. Note the following two files that will be used in CloudFront distribution configuration

      - index.html

      - 404.html

### Create an S3 bucket for the website

1. Set the target AWS profile

   *Format:*
   
   ```ShellSession
   $ export AWS_PROFILE={target aws profile}
   ```

   *Example for semanticbitsdemo:*
   
   ```ShellSession
   $ export AWS_PROFILE=sbdemo-shared
   ```

1. Create S3 bucket for the website

   *Format:*
   
   ```ShellSession
   $ aws --region us-east-1 s3api create-bucket \
     --bucket {unique id}-ab2d-website
   ```

   *Example for semanticbitsdemo:*
   
   ```ShellSession
   $ aws --region us-east-1 s3api create-bucket \
     --bucket sbdemo-ab2d-website
   ```

1. Block public access on the bucket

   *Format:*
   
   ```ShellSession
   $ aws --region us-east-1 s3api put-public-access-block \
      --bucket {unique id}-ab2d-website \
      --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

   *Example for semanticbitsdemo:*
   
   ```ShellSession
   $ aws --region us-east-1 s3api put-public-access-block \
      --bucket sbdemo-ab2d-website \
      --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```
   
### Upload website to S3

1. Note that the uploaded website will be used to create an S3 API endpoint as the origin within CloudFront

1. Change to the "website" directory

   ```ShellSession
   $ cd ~/code/ab2d/website
   ```

1. Set the target AWS profile

   *Format:*
   
   ```ShellSession
   $ export AWS_PROFILE={target aws profile}
   ```

   *Example for semanticbitsdemo:*
   
   ```ShellSession
   $ export AWS_PROFILE=sbdemo-shared
   ```

1. Upload website to S3

   *Format:*
   
   ```ShellSession
   $ aws s3 cp --recursive _site/ s3://{unique id}-ab2d-website/
   ```

   *Example for semanticbitsdemo:*
   
   ```ShellSession
   $ aws s3 cp --recursive _site/ s3://sbdemo-ab2d-website/
   ```

### Create CloudFront distribution

1. Open Chrome

1. Log on to AWS

1. Navigate to CloudFront

1. Select **Create Distribution**

1. Select **Get Started** under the *Web* section

1. Note the following very important information before configuring the "Origin Settings"

   1. Note that there are two "Origin Domain Name" values that can be used for doing a distribution for an S3 website

      - S3 API Endpoint <-- this is the method that we want to use

      - S3 Website Endpoint

   1. Note that the "Origin ID" will automatically fill-in based on the "Origin Domain Name"
   
1. Configure "Origin Settings" as follows:

   *Format:*
   
   - **Origin Domain Name:** {unique id}-ab2d-website.s3.amazonaws.com

   - **Origin ID:** S3-{unique id}-ab2d-website

   - **Restrict Bucket Access:** Yes

   - **Origin Access Identity:** Create a New Identity

   - **Comment:** access-identity-{unique id}-ab2d-website.s3.amazonaws.com

   - **Grant Read Permissions on Bucket:** Yes, Update Bucket Policy

   *Example for semanticbitsdemo:*
   
   - **Origin Domain Name:** sbdemo-ab2d-website.s3.amazonaws.com
   
   - **Origin ID:** S3-sbdemo-ab2d-website

   - **Restrict Bucket Access:** Yes

   - **Origin Access Identity:** Create a New Identity

   - **Comment:** access-identity-sbdemo-ab2d-website.s3.amazonaws.com

   - **Grant Read Permissions on Bucket:** Yes, Update Bucket Policy

1. Configure "Default Cache Behavior Settings" as follows

   - **Viewer Protocol Policy:** Redirect HTTP to HTTPS

1. If you are NOT using a custom SSL certificate, configure "Distribution Settings" as follows

   *Example for semanticbitsdemo:*
   
   **Alternate Domain Names (CNAMES):** {blank}

   **SSL Certificate:** Default CloudFront Certificate

   **Default Root Object:** index.html

1. If you are using a custom SSL certificate, configure "Distribution Settings" as follows

   *Format:*
   
   **Alternate Domain Names (CNAMES):** ab2d.{domain}

   **SSL Certificate:** Custom SSL Certificate

   **Custom SSL Certificate:** ab2d.{domain}

   **Default Root Object:** index.html

1. Select **Create Distribution**

1. Select **Distributions** in the leftmost panel

1. Note the distribution row that was created

   *Format:*
   
   - **Delivery Method:** Web

   - **Domain Name:** {unique id}.cloudfront.net

   - **Origin:** sbdemo-ab2d-website.s3.amazonaws.com

1. Note that it will take about 30 minutes for the CloudFront distribution to complete

1. Wait for the "Status" to change to the following

   ```
   Deployed
   ```

1. If you have access to a domain that you can personally administer in Route 53 and want to complete the test setup, jump to the following section:

   [Update Route 53 DNS record to point custom CNAME to the CloudFront distribution](#update-route-53-dns-record-to-point-custom-cname-to-the-cloudfront-distribution)

1. If you just want to test setting up an S3 website with a CloudFront distribution, jump to the following section:

   [Test the CloudFront distribution](#test-the-cloudfront-distribution)
   
### Update Route 53 DNS record to point custom CNAME to the CloudFront distribution

1. Open Chrome

1. Log on to AWS

1. Navigate to Route 53

1. Select **Hosted Zones** in the leftmost panel

1. Select the desired domain from the list of domains

1. Select **Create Record Set**

1. Configure the record set as follows

   - **Name:** ab2d

   - **Type:** A - IpV4 address

   - **Alias:** Yes

   - **Alias Target:** ab2d.{domain}. ({unique id}.cloudfront.net)

1. Select **Create**

### Test the CloudFront distribution

1. Note the CloudFront domain for the CloudFront distribution

   *Format:*
   
   ```
   {unique id}.cloudfront.net
   ```

1. Open Chrome

1. Test the CloudFront domain

   *Format:*
   
   > https://{unique id}.cloudfront.net

1. Verify that the website from S3 is displayed

1. If you used a custom SSL certificate, do the following

   1. Note that you may have to wait some time until to allow DNS to propagate

   1. Open Chrome

   1. Test the DNS with HTTPS

      > https://ab2d.{domain}

## Appendix Q: Create a VPC network peering test

1. Set the AWS profile

   ```ShellSession
   $ export AWS_PROFILE=sbdemo-shared
   ```

1. Create keypair

   *Example for controllers within SemanticBits demo environment:*

   ```ShellSession
   $ aws --region ca-central-1 ec2 create-key-pair \
     --key-name vpc-peering-test \
     --query 'KeyMaterial' \
     --output text \
     > ~/.ssh/vpc-peering-test.pem
   ```

1. Change permissions of the key

   *Example for controllers within SemanticBits demo environment:*

   ```ShellSession
   $ chmod 600 ~/.ssh/vpc-peering-test.pem
   ```

1. Output the public key to the clipboard

   *Example for controllers within SemanticBits demo environment:*

   ```ShellSession
   $ ssh-keygen -y -f ~/.ssh/vpc-peering-test.pem | pbcopy
   ```

1. Update the "authorized_keys" file for the environment

   1. Open the "authorized_keys" file for the environment
   
      ```ShellSession
      $ vim ~/code/ab2d/Deploy/terraform/environments/vpc-peering-test/authorized_keys
      ```

   1. Paste the public key under the "Keys included with CentOS image" section

   1. Save and close the file

1. Change to the deploy directory

   *Format:*
   
   ```ShellSession
   $ cd {code directory}/ab2d/Deploy
   ```

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Create the VPC for the dev environment
   
   ```ShellSession
   $ ./create-vpc-for-sbdemo.sh \
     --environment=dev \
     --region=ca-central-1 \
     --vpc-cidr-block-1=10.242.26.0/24 \
     --vpc-cidr-block-2=10.242.5.128/26 \
     --subnet-public-1-cidr-block=10.242.5.128/27 \
     --subnet-public-2-cidr-block=10.242.5.160/27 \
     --subnet-private-1-cidr-block=10.242.26.0/25 \
     --subnet-private-2-cidr-block=10.242.26.128/25
   ```

1. Create the VPC for the sbx environment

   > *** TO DO ***: Change CIDR values to match assigned values from CMS AWS account
   
   ```ShellSession
   $ ./create-vpc-for-sbdemo.sh \
     --environment=sbx \
     --region=ca-central-1 \
     --vpc-cidr-block-1=10.242.27.0/24 \
     --vpc-cidr-block-2=10.242.6.128/26 \
     --subnet-public-1-cidr-block=10.242.6.128/27 \
     --subnet-public-2-cidr-block=10.242.6.160/27 \
     --subnet-private-1-cidr-block=10.242.27.0/25 \
     --subnet-private-2-cidr-block=10.242.27.128/25
   ```

1. Deploy components for VPC peering test

   ```ShellSession
   $ ./bash/vpc-peering-test.sh \
     --environment-1=dev \
     --region-1=ca-central-1 \
     --environment-2=sbx \
     --region-2=ca-central-1 \
     --seed-ami-product-code=aw0evgkw8e5c1q413zgy5pjce \
     --ssh-username=centos \
     --ec2-instance-type=m5.xlarge
   ```