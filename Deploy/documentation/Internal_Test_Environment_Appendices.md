# Internal Test Environment Appendices

## Table of Contents

1. [Appendix A: Destroy complete environment](#appendix-a-destroy-complete-environment)
1. [Appendix B: Retest terraform using existing AMI](#appendix-b-retest-terraform-using-existing-ami)
1. [Appendix C: Stop and restart the ECS cluster](#appendix-c-stop-and-restart-the-ecs-cluster)
1. [Appendix D: Create an S3 bucket with AWS CLI](#appendix-d-create-an-s3-bucket-with-aws-cli)
1. [Appendix E: Verify EFS mounting on worker node](#appendix-e-verify-efs-mounting-on-worker-node)
1. [Appendix F: Verify PostgreSQL](#appendix-f-verify-postgresql)
1. [Appendix G: Note the product code for CentOS 7 AMI](#appendix-g-note-the-product-code-for-centos-7-ami)
1. [Appendix H: Do a linting check of the terraform files](#appendix-h-do-a-linting-check-of-the-terraform-files)
1. [Appendix I: Configure the controller to test docker containers](#appendix-i-configure-the-controller-to-test-docker-containers)
1. [Appendix J: Manual create of AWS Elastic Container Registry repositories for images](#appendix-j-manual-create-of-aws-elastic-container-registry-repositories-for-images)
1. [Appendix K: Manually create an ECS cluster](#appendix-k-manually-create-an-ecs-cluster)
1. [Appendix L: Set up a PagerDuty free trial](#appendix-l-set-up-a-pagerduty-free-trial)

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
   $ ./destroy-environment.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared
   ```

   *Example to destroy the environment, but preserve the AMIs:*
   
   ```ShellSession
   $ ./destroy-environment.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --keep-ami
   ```

   *Example to destroy the environment, but preserve the networking:*
   
   ```ShellSession
   $ ./destroy-environment.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --keep-network
   ```

   *Example to destroy the environment, but preserve both the AMIs and the networking:*
   
   ```ShellSession
   $ ./destroy-environment.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --keep-ami \
     --keep-network
   ```

1. Destroy the "sbdemo-sbx" environment

   *Example for Sandbox environment testing within SemanticBits demo environment:*

   ```ShellSession
   $ ./destroy-environment.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared
   ```

   *Example to destroy the environment, but preserve the AMIs:*
   
   ```ShellSession
   $ ./destroy-environment.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared \
     --keep-ami
   ```

   *Example to destroy the environment, but preserve the networking:*
   
   ```ShellSession
   $ ./destroy-environment.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared \
     --keep-network
   ```

   *Example to destroy the environment, but preserve both the AMIs and the networking:*
   
   ```ShellSession
   $ ./destroy-environment.sh \
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

## Appendix H: Do a linting check of the terraform files

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Do a linting check

   ```ShellSession
   $ ./bash/tflint-check.sh
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

   ```ShellSession
   $ docker tag ab2d_api:latest 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_api:latest
   ```

1. Push the "ab2d_api" image to ECR

   ```ShellSession
   $ docker push 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_api:latest
   ```

1. Create an AWS Elastic Container Registry (ECR) for "ab2d_worker"

   ```ShellSession
   $ aws ecr create-repository --repository-name ab2d_worker
   ```

1. Tag the "ab2d_worker" image for ECR

   ```ShellSession
   $ docker tag ab2d_worker:latest 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_worker:latest
   ```

1. Push the "ab2d_worker" image to ECR

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

## Appendix L: Set up a PagerDuty free trial

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

1. Sign in with your new credentials

   - **Email:** {your semanticbits email)

   - **Password:** {your desired password}

1. Select the [Apps](images/pager-duty-apps-icon.png) icon

1. Select **Integrations Directory**

1. Note that a new Chrome tab will automatically open

1. Scroll down to the "Intergration Library" section

1. Select **Amazon CloudWatch**

1. Note that the following page will open

   > https://support.pagerduty.com/docs/aws-cloudwatch-integration-guide

