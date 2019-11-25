# Notes

## Table of Contents

1. [To do after granted access to AWS account](#to-do-after-granted-access-to-aws-account)
1. [To do later](#to-do-later)
1. [Preparation notes](#preparation-notes)
1. [AB2D application DevOps integration notes](#ab2d-application-devops-integration-notes)
1. [Issues](#issues)
   * [Network Load Balancer bucket policy](#network-load-balancer-bucket-policy)
   * [EFS mounting](#efs-mounting)
   * [Resolve repeated start and stop of ECS tasks](#resolve-repeated-start-and-stop-of-ecs-tasks)
   * [Separate API and Worker into separate AMIs](#separate-api-and-worker-into-separate-amis)

## To do after granted access to AWS account

1. Complete "Setup develoment machine" section for AWS access

1. Verify the gold AMI

   ```
   ami-01962ba1fa3692903
   ```
   
1. Open "app.json" for "app" under packer

   ```ShellSession
   $ vim ./Deploy/packer/app/app.json
   ```

1. Update the following

   ```
   "gold_ami": "ami-01962ba1fa3692903"
   "subnet_id": "SUBNET_ID",
   "vpc_id": "VPC_ID",
   ```

1. Open "app.json" for "app" under packer

   ```ShellSession
   $ vim ./Deploy/packer/app/splunk-deploymentclient.conf
   ```

1. Update the following

   ```
   targetUri = DEPLOYMENT_SERVER_PRIVATE_IP:8089
   ```

1. Open "variables.tf" for DEV environment

   ```ShellSession
   $ vim ./Deploy/terraform/cms-ab2d-dev/variables.tf
   ```

1. Change the following sections

   ```
   variable "vpc_id" {
     default = "vpc-09a9ad141cc53e39d"
   }

   variable "private_subnet_ids" {
     type        = list(string)
     default     = ["subnet-078ffc37780c010af", "subnet-030e47900032cb0bb", "subnet-0c14499ea66bd9dc1"]
     description = "App instances and DB go here"
   }
   
   variable "deployment_controller_subnet_ids" {
     type        = list(string)
     default     = ["subnet-0169444a442ed9d28", "subnet-0581df111f086f55b", "subnet-091dde638f2bc571d"]
     description = "Deployment controllers go here"
   }

   variable "s3_username_whitelist" {
     default = ["HV7K"]
   }

   variable "gold_image_name" {
     default = "EAST-RH 7-6 Gold Image V.1.09 (HVM) 06-26-19"
   }
   
   variable "enterprise-tools-sec-group-id" {
     default = "sg-0566ad330966d8ba7"
   }
   
   variable "vpn-private-sec-group-id" {
     default = "sg-07fbbd710a8b15851"
   }
   ```
   
## To do later

1. Open "provision-app-instance.sh"

   ```ShellSession
   $ vim ./Deploy/packer/app/provision-app-instance.sh
   ```

1. Uncomment and work on the "Install newrelic infrastructure agent" section

   *Note that the section references an encrypted file that is copied from S3.*

## Preparation notes

1. Change to the repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Backup the "app.json" file to be used for the government AWS account

   ```ShellSession
   $ cp Deploy/packer/app/app.json Deploy/packer/app/app.json.gov
   ```

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Get the latest CentOS AMI

   ```ShellSession
   $ aws --region us-east-1 ec2 describe-images \
     --owners aws-marketplace \
     --filters Name=product-code,Values=aw0evgkw8e5c1q413zgy5pjce \
     --query 'Images[*].[ImageId,CreationDate]' \
     --output text \
     | sort -k2 -r \
     | head -n1
   ```

1. Note the AMI in the output

   *Example:*
   
   ```
   ami-02eac2c0129f6376b
   ```

1. Open the "app.json" file

   ```ShellSession
   $ vim Deploy/packer/app/app.json
   ```

1. Change the following with the noted AMI

   *Example:*
   
   ```
   "gold_ami": "ami-02eac2c0129f6376b"
   ```

1. Change the networking settings based on the VPC that was created

   *Example:*
   
   ```
   "subnet_id": "subnet-0b8ba5ef9b89b07ed",
   "vpc_id": "vpc-064c3621b7205922a",
   ```

1. Change the builders settings

   *Example:*

   ```
   "iam_instance_profile": "lonnie.hanekamp@semanticbits.com",
   "ssh_username": "centos",
   ```

1. Save and close "app.json"

1. Backup the "provision-app-instance.sh" file to be used for the government AWS account

   ```ShellSession
   $ cp Deploy/packer/app/provision-app-instance.sh Deploy/packer/app/provision-app-instance.sh.gov
   ```

1. Open "provision-app-instance.sh"

   ```ShellSession
   $ vim Deploy/packer/app/provision-app-instance.sh
   ```

1. Comment out gold disk related items

   1. Comment out "Update splunk forwarder config" section

      ```
      #
      # LSH Comment out gold disk related section
      #
      # # Update splunk forwarder config
      # sudo chown splunk:splunk /tmp/splunk-deploymentclient.conf
      # sudo mv -f /tmp/splunk-deploymentclient.conf /opt/splunkforwarder/etc/system/local/deploymentclient.conf
      ```

   1. Comment out "Make sure splunk can read all logs" section

      ```
      #
      # LSH Comment out gold disk related section
      #
      # # Make sure splunk can read all logs
      # sudo /opt/splunkforwarder/bin/splunk stop
      # sudo /opt/splunkforwarder/bin/splunk clone-prep-clear-config
      # sudo /opt/splunkforwarder/bin/splunk start
      ```

1. Save and close "provision-app-instance.sh"

## AB2D application DevOps integration notes

1. Open Chrome

1. Review Docker compose file

   1. Enter the following in the address bar

      > https://github.com/CMSgov/ab2d/blob/master/docker-compose.yml

   1. Note the contents

      *Example at time of writing:*

      ```
      version: '3'
      
      services:
        db:
          image: postgres:11
          environment:
            - POSTGRES_DB=ab2d
            - POSTGRES_USER=ab2d
            - POSTGRES_PASSWORD=ab2d
          ports:
            - "5432:5432"
        build:
          image: maven:3-jdk-12
          working_dir: /usr/src/mymaven
          command: mvn clean package
          volumes:
            - .:/usr/src/mymaven
            - ${HOME}/.m2:/root/.m2
        api:
          build:
            context: ./api
          environment:
            - AB2D_DB_HOST=db
            - AB2D_DB_PORT=5432
            - AB2D_DB_DATABASE=ab2d
            - AB2D_DB_USER=ab2d
            - AB2D_DB_PASSWORD=ab2d
          ports:
            - "8080:8080"
          depends_on:
            - db
        worker:
          build:
            context: ./worker
          environment:
            - AB2D_DB_HOST=db
            - AB2D_DB_PORT=5432
            - AB2D_DB_DATABASE=ab2d
            - AB2D_DB_USER=ab2d
            - AB2D_DB_PASSWORD=ab2d
          depends_on:
            - db
      ```

1. Review API dockerfile

   1. Enter the following in the address bar

      > https://github.com/CMSgov/ab2d/blob/master/api/Dockerfile

   2. Note the contents

      *Example at time of writing:*
      
      ```
      FROM openjdk:12
      WORKDIR /usr/src/ab2d-api
      ADD target /usr/src/ab2d-api
      CMD java -jar api-*-SNAPSHOT.jar
      EXPOSE 8080
      ```

1. Review worker dockerfile

   1. Enter the following in the address bar

      > https://github.com/CMSgov/ab2d/blob/master/worker/Dockerfile

   2. Note the contents

      *Example at time of writing:*
      
      ```
      FROM openjdk:12
      WORKDIR /usr/src/ab2d-worker
      ADD target /usr/src/ab2d-worker
      CMD java -jar worker-*-SNAPSHOT.jar
      ```

1. Review API application properties

   1. Enter the following in the address bar

      > https://github.com/CMSgov/ab2d/blob/master/api/src/main/resources/application.properties

   2. Note the contents

      *Example at time of writing:*
      
      ```
      spring.datasource.url=jdbc:postgresql://${AB2D_DB_HOST}:${AB2D_DB_PORT}/${AB2D_DB_DATABASE}
      spring.datasource.username=${AB2D_DB_USER}
      spring.datasource.password=${AB2D_DB_PASSWORD}
      spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false
      spring.datasource.driver-class-name=org.postgresql.Driver
      spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
      
      spring.jpa.hibernate.ddl-auto=validate
      spring.integration.jdbc.initialize-schema=always
      
      api.retry-after.delay=30
      
      
      ## -----------------------------------------------------------------------------------------------------  LOGGING LEVEL
      logging.level.root=INFO
      logging.level.gov.cms.ab2d=INFO
      
      logging.level.org.springframework=WARN
      logging.level.com.zaxxer.hikari=WARN
      logging.level.org.hibernate=WARN
      logging.level.liquibase=WARN
      ```

1. Review common application properties

   1. Enter the following in the address bar

      > https://github.com/CMSgov/ab2d/blob/master/common/src/main/resources/application.common.properties

   2. Note the contents

      *Example at time of writing:*
      
      ```
      efs.mount=${java.io.tmpdir}/jobdownloads/
      ```

1. Review database changelog master

   1. Enter the following in the address bar

      > https://github.com/CMSgov/ab2d/blob/master/common/src/main/resources/db/changelog/db.changelog-master.yaml

   2. Note the contents

      *Example at time of writing:*
      
      ```
      # May need to change this in future if there's another directory besides v001
      databaseChangeLog:
        - include:
            file: db/changelog/v001/AB2D-291-create-initial-tables.sql
        - include:
            file: db/changelog/v001/AB2D-331-drop-constraint-add-column.sql
      ```

1. Review the "AB2D-291-create-initial-tables.sql" script

   1. Enter the following in the address bar

      > https://github.com/CMSgov/ab2d/blob/master/common/src/main/resources/db/changelog/v001/AB2D-291-create-initial-tables.sql

   2. Note the contents

      *Example at time of writing:*
      
      ```
      --liquibase formatted sql
      --  -------------------------------------------------------------------------------------------------------------------
      
      
      --changeset spathiyil:AB2D-291-CreateSequence-hibernate_sequence failOnError:true dbms:postgresql
      CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;
      
      
      --changeset spathiyil:AB2D-291-CreateTable-beneficiary failOnError:true dbms:postgresql
      CREATE TABLE beneficiary
      (
          id                  BIGINT              NOT NULL,
          patient_id          VARCHAR(255)        NOT NULL
      );
      
      ALTER TABLE beneficiary ADD CONSTRAINT "pk_beneficiary" PRIMARY KEY (id);
      ALTER TABLE beneficiary ADD CONSTRAINT "uc_beneficiary_patient_id" UNIQUE (patient_id);
      
      --rollback DROP TABLE beneficiary;
      --  -------------------------------------------------------------------------------------------------------------------
      
      
      --changeset spathiyil:AB2D-291-CreateTable-contract failOnError:true dbms:postgresql
      CREATE TABLE contract
      (
          id                  BIGINT              NOT NULL,
          contract_id         VARCHAR(255)        NOT NULL
      );
      
      ALTER TABLE contract ADD CONSTRAINT "pk_contract" PRIMARY KEY (id);
      ALTER TABLE contract ADD CONSTRAINT "uc_contract_contract_id" UNIQUE (contract_id);
      
      --rollback DROP TABLE contract;
      --  -------------------------------------------------------------------------------------------------------------------
      
      
      --changeset spathiyil:AB2D-291-CreateTable-coverage failOnError:true dbms:postgresql
      CREATE TABLE coverage
      (
          beneficiary_id      BIGINT              NOT NULL,
          contract_id         BIGINT              NOT NULL
      );
      
      ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_beneficiary" FOREIGN KEY (beneficiary_id) REFERENCES beneficiary (id);
      ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_contract" FOREIGN KEY (contract_id) REFERENCES contract (id);
      ALTER TABLE coverage ADD CONSTRAINT "uc_coverage_beneficiary_id_contract_id" UNIQUE (beneficiary_id, contract_id);
      
      --rollback  DROP TABLE coverage;
      --  -------------------------------------------------------------------------------------------------------------------
      
      
      --changeset spathiyil:AB2D-291-CreateTable-sponsor failOnError:true dbms:postgresql
      CREATE TABLE sponsor
      (
          id                  BIGINT              NOT NULL,
          hpms_id             INTEGER             NOT NULL,
          org_name            VARCHAR(255)        NOT NULL,
          legal_name          VARCHAR(255),
          parent_id           BIGINT
      );
      
      ALTER TABLE sponsor ADD CONSTRAINT "pk_sponsor" PRIMARY KEY (id);
      ALTER TABLE sponsor ADD CONSTRAINT "uc_sponsor_hpms_id" UNIQUE (hpms_id);
      ALTER TABLE sponsor ADD CONSTRAINT "fk_sponsor_to_sponsor_parent" FOREIGN KEY (parent_id) REFERENCES sponsor (id);
      
      --rollback DROP TABLE sponsor;
      --  -------------------------------------------------------------------------------------------------------------------
      
      
      --changeset spathiyil:AB2D-291-CreateTable-attestation failOnError:true dbms:postgresql
      CREATE TABLE attestation
      (
          id                  BIGINT              NOT NULL,
          sponsor_id          BIGINT              NOT NULL,
          contract_id         BIGINT              NOT NULL,
          attested_on         TIMESTAMPTZ
      );
      
      ALTER TABLE attestation ADD CONSTRAINT "pk_attestation" PRIMARY KEY (id);
      ALTER TABLE attestation ADD CONSTRAINT "fk_attestation_to_sponsor"  FOREIGN KEY (sponsor_id) REFERENCES sponsor (id);
      ALTER TABLE attestation ADD CONSTRAINT "fk_attestation_to_contract" FOREIGN KEY (contract_id) REFERENCES contract (id);
      
      --rollback DROP TABLE attestation;
      --  -------------------------------------------------------------------------------------------------------------------
      
      
      --changeset spathiyil:AB2D-291-CreateTable-user_account failOnError:true dbms:postgresql
      CREATE TABLE user_account
      (
          id                  BIGINT              NOT NULL,
          username           VARCHAR(64)         NOT NULL,
          first_name          VARCHAR(64),
          last_name           VARCHAR(64),
          email               VARCHAR(255),
          sponsor_id          BIGINT              NOT NULL,
          enabled             BOOLEAN             NOT NULL
      );
      
      ALTER TABLE user_account ADD CONSTRAINT "pk_user_account" PRIMARY KEY (id);
      ALTER TABLE user_account ADD CONSTRAINT "uc_user_account_username" UNIQUE (username);
      ALTER TABLE user_account ADD CONSTRAINT "uc_user_account_email" UNIQUE (email);
      ALTER TABLE user_account ADD CONSTRAINT "fk_user_account_to_sponsor"  FOREIGN KEY (sponsor_id) REFERENCES sponsor (id);
      
      
      --rollback DROP TABLE user_account;
      --  -------------------------------------------------------------------------------------------------------------------
      
      
      --changeset spathiyil:AB2D-291-CreateTable-role failOnError:true dbms:postgresql
      CREATE TABLE role
      (
          id                  BIGINT              NOT NULL,
          name                VARCHAR(64)         NOT NULL
      );
      
      ALTER TABLE role ADD CONSTRAINT "pk_role" PRIMARY KEY (id);
      ALTER TABLE role ADD CONSTRAINT "uc_role_name" UNIQUE (name);
      
      
      --rollback DROP TABLE role;
      --  -------------------------------------------------------------------------------------------------------------------
      
      
      --changeset spathiyil:AB2D-291-CreateTable-user_role failOnError:true dbms:postgresql
      CREATE TABLE user_role
      (
          user_account_id     BIGINT              NOT NULL,
          role_id             BIGINT              NOT NULL
      );
      
      ALTER TABLE user_role ADD CONSTRAINT "fk_user_role_to_user_account" FOREIGN KEY (user_account_id) REFERENCES user_account (id);
      ALTER TABLE user_role ADD CONSTRAINT "fk_user_role_to_role" FOREIGN KEY (role_id) REFERENCES role (id);
      ALTER TABLE user_role ADD CONSTRAINT "uc_user_role_user_account_id_role_id" UNIQUE (user_account_id, role_id);
      
      
      --rollback DROP TABLE user_role;
      --  -------------------------------------------------------------------------------------------------------------------
      
      
      --changeset spathiyil:AB2D-291-CreateTable-job failOnError:true dbms:postgresql
      CREATE TABLE job
      (
          id                  BIGINT              NOT NULL,
          job_id              VARCHAR(255)        NOT NULL,
          user_account_id     BIGINT              NOT NULL,
          created_at          TIMESTAMPTZ         NOT NULL,
          expires_at          TIMESTAMPTZ         NOT NULL,
          resource_types      VARCHAR(255)        NOT NULL,
          status              VARCHAR(32)         NOT NULL,
          status_message      TEXT,
          request_url         TEXT,
          progress            INT,
          last_poll_time      TIMESTAMPTZ,
          completed_at        TIMESTAMPTZ
      );
      
      ALTER TABLE job ADD CONSTRAINT "pk_job" PRIMARY KEY (id);
      ALTER TABLE job ADD CONSTRAINT "uc_job_job_id" UNIQUE (job_id);
      
      ALTER TABLE job ADD CONSTRAINT "fk_job_to_user_account" FOREIGN KEY (user_account_id) REFERENCES user_account (id);
      
      --rollback DROP TABLE job;
      --  -------------------------------------------------------------------------------------------------------------------
      
      
      --changeset spathiyil:AB2D-291-CreateTable-job_output failOnError:true dbms:postgresql
      CREATE TABLE job_output
      (
          id                  BIGINT              NOT NULL,
          job_id              BIGINT              NOT NULL,
          file_path           TEXT                NOT NULL,
          fhir_resource_type  VARCHAR(255)        NOT NULL,
          error               BOOLEAN             NOT NULL
      );
      
      ALTER TABLE job_output ADD CONSTRAINT "pk_job_output" PRIMARY KEY (id);
      ALTER TABLE job_output ADD CONSTRAINT "fk_job_output_to_job" FOREIGN KEY (job_id) REFERENCES job (id);
      
      --rollback DROP TABLE job_output;
      ```

1. Review the "AB2D-331-drop-constraint-add-column.sql" script

   1. Enter the following in the address bar

      > https://github.com/CMSgov/ab2d/blob/master/common/src/main/resources/db/changelog/v001/AB2D-331-drop-constraint-add-column.sql

   2. Note the contents

      *Example at time of writing:*
      
      ```
      --liquibase formatted sql
      --  -------------------------------------------------------------------------------------------------------------------
      
      --changeset adaykin:AB2D-331-DropUCSponsorHMPSIDConstraint failOnError:true dbms:postgresql
      ALTER TABLE sponsor DROP CONSTRAINT "uc_sponsor_hpms_id";
      
      --changeset adaykin:AB2D-331-AddContractName failOnError:true dbms:postgresql
      ALTER TABLE contract ADD COLUMN contract_name VARCHAR(128) NOT NULL;
      ```

## Issues

### Network Load Balancer bucket policy

1. Note that that the following policy statement allows logging from an application load balancer

   ```
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Principal": {
           "AWS": "arn:aws:iam::127311923021:root"
         },
         "Action": "s3:PutObject",
         "Resource": "arn:aws:s3:::cms-ab2d-cloudtrail/*"
       }
     ]
   }
   ```

1. Note that that the following policy statement allows logging from a network load balancer

   ```
   {
       "Version": "2012-10-17",
       "Id": "AWSConsole-AccessLogs-Policy-1571098355053",
       "Statement": [
           {
               "Sid": "AWSConsoleStmt-1571098355053",
               "Effect": "Allow",
               "Principal": {
                   "AWS": "arn:aws:iam::127311923021:root"
               },
               "Action": "s3:PutObject",
               "Resource": "arn:aws:s3:::cms-ab2d-cloudtrail-nlb/AWSLogs/114601554524/*"
           },
           {
               "Sid": "AWSLogDeliveryWrite",
               "Effect": "Allow",
               "Principal": {
                   "Service": "delivery.logs.amazonaws.com"
               },
               "Action": "s3:PutObject",
               "Resource": "arn:aws:s3:::cms-ab2d-cloudtrail-nlb/AWSLogs/114601554524/*",
               "Condition": {
                   "StringEquals": {
                       "s3:x-amz-acl": "bucket-owner-full-control"
                   }
               }
           },
           {
               "Sid": "AWSLogDeliveryAclCheck",
               "Effect": "Allow",
               "Principal": {
                   "Service": "delivery.logs.amazonaws.com"
               },
               "Action": "s3:GetBucketAcl",
               "Resource": "arn:aws:s3:::cms-ab2d-cloudtrail-nlb"
           }
       ]
   }
   ```

1. Note that the statements are combined for a single bucket policy in the next step

1. Add this bucket policy to the "cms-ab2d-cloudtrail" S3 bucket via the AWS console

   > *** TO DO ***: Need to script this using AWS CLI
   
   ```
   {
       "Version": "2012-10-17",
       "Statement": [
           {
               "Effect": "Allow",
               "Principal": {
                   "AWS": "arn:aws:iam::127311923021:root"
               },
               "Action": "s3:PutObject",
               "Resource": "arn:aws:s3:::cms-ab2d-cloudtrail/*"
           },
           {
               "Sid": "AWSLogDeliveryWrite",
               "Effect": "Allow",
               "Principal": {
                   "Service": "delivery.logs.amazonaws.com"
               },
               "Action": "s3:PutObject",
               "Resource": "arn:aws:s3:::cms-ab2d-cloudtrail/nlb/AWSLogs/114601554524/*",
               "Condition": {
                   "StringEquals": {
                       "s3:x-amz-acl": "bucket-owner-full-control"
                   }
               }
           },
           {
               "Sid": "AWSLogDeliveryAclCheck",
               "Effect": "Allow",
               "Principal": {
                   "Service": "delivery.logs.amazonaws.com"
               },
               "Action": "s3:GetBucketAcl",
               "Resource": "arn:aws:s3:::cms-ab2d-cloudtrail"
           }
       ]
   }
   ```

### EFS mounting

> https://docs.aws.amazon.com/efs/latest/ug/efs-mount-helper.html

> https://docs.aws.amazon.com/efs/latest/ug/mounting-fs.html

### Resolve repeated start and stop of ECS tasks

> *** TO DO ***

### Separate API and Worker into separate AMIs

*Need to separate the AMIs because only Worker needs EFS mounting.*
