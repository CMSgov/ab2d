# Mac User Resarcher SemanticBits

## Table of Contents

1. [Setup Mac](#setup-mac)
1. [Use an SSH tunnel to query sandbox database from local machine](#use-an-ssh-tunnel-to-query-sandbox-database-from-local-machine)
1. [Use an SSH tunnel to query production database from local machine](#use-an-ssh-tunnel-to-query-production-database-from-local-machine)

## Setup Mac

1. Install pgAdmin

   > https://www.pgadmin.org/download/

1. Download the following keys from 1Password to the "~/Downloads" directory

   Label                                  |File
   ---------------------------------------|--------------------
   AB2D Sbx : EC2 Instances : Private Key |ab2d-sbx-sandbox.pem
   AB2D Prod : EC2 Instances : Private Key|ab2d-east-prod.pem

1. Configure SSH

   ```ShellSession
   $ mkdir -p ~/.ssh \
     && cp ~/Downloads/ab2d-sbx-sandbox.pem ~/.ssh \
     && chmod 600 ~/.ssh/ab2d-sbx-sandbox.pem \
     && cp ~/Downloads/ab2d-east-prod.pem ~/.ssh \
     && chmod 600 ~/.ssh/ab2d-east-prod.pem
   ```

1. Install python3 (see Mac_Developer_SemanticBits.md)

1. Configure pip3 (see Mac_Developer_SemanticBits.md)

1. Install AWS CLI (see Mac_Developer_SemanticBits.md)

1. Install jq (see Mac_Developer_SemanticBits.md)

## Use an SSH tunnel to query sandbox database from local machine

1. If you have any pgAdmin client Chrome tabs open, close them before proceeding

1. If you have an existing database tunnel running in the terminal, do the following:

   1. Select the "** WARNING ** WARNING ** WARNING ** WARNING ** WARNING" terminal tab where the database tunnel is running

   1. Press **control+C** on the keyboard

1. Ensure that the pgAdmin server is running

   1. Note that if you see an elephant icon in the top right system bar, this means pgAdmin server is running

   1. If pgAdmin is not running or you are unsure if it is running, do the following:

      1. Select **Launchpad**

      1. Select **pgAdmin 4**

1. Select **Launchpad**

1. Select **Terminal**

1. Open Chrome

1. Get temporary credentials for sandbox

   1. Open CloudTamer

      > https://cloudtamer.cms.gov/portal

   1. Select **Projects** from the leftmost panel

   1. Select **Account Access** beside "AB2D Sandbox"

   1. Select **aws-cms-oit-iusg-acct95 (777200079629)**

   1. Select **AB2D Application Admin**

   1. Select **Short-term Access Keys**

   1. Wait for access keys to generate

   1. Select the gray box under the "Option 1" section to copy the export variables to the clipboard

   1. Return to the terminal that you opened

   1. Paste the contents of the clipboard to the terminal

   1. Press "return" on the keyboard

1. Change to your "ab2d" repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```
   
1. Enter the following in the terminal to start the database tunnel

   ```ShellSession
   $ LOCAL_DB_PORT=1234 \
     && CONTROLLER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text) \
     &&  DATABASE_SECRET_DATETIME="2020-01-02-09-15-01" \
     && DATABASE_HOST=$(./Deploy/python3/get-database-secret.py ab2d-sbx-sandbox database_host $DATABASE_SECRET_DATETIME) \
     && ssh -N -L \
     "${LOCAL_DB_PORT}:${DATABASE_HOST}:5432" \
     ec2-user@"${CONTROLLER_PRIVATE_IP}" \
     -i "~/.ssh/ab2d-sbx-sandbox.pem"
   ```

1. Open a pgAdmin client in a Chrome tab by doing the following

   1. Select the elephant icon in the top right system bar

   1. Select **New pgAdmin 4 window**

## Use an SSH tunnel to query production database from local machine

1. If you have any pgAdmin client Chrome tabs open, close them before proceeding

1. If you have an existing database tunnel running in the terminal, do the following:

   1. Select the "** WARNING ** WARNING ** WARNING ** WARNING ** WARNING" terminal tab where the database tunnel is running

   1. Press **control+C** on the keyboard

1. Ensure that the pgAdmin server is running

   1. Note that if you see an elephant icon in the top right system bar, this means pgAdmin server is running

   1. If pgAdmin is not running or you are unsure if it is running, do the following:

      1. Select **Launchpad**

      1. Select **pgAdmin 4**

1. Select **Launchpad**

1. Select **Terminal**

1. Open Chrome

1. Get temporary credentials for Production

   1. Open CloudTamer

      > https://cloudtamer.cms.gov/portal

   1. Select **Projects** from the leftmost panel

   1. Select **Account Access** beside "AB2D Prod"

   1. Select **aws-cms-oit-iusg-acct98 (595094747606)**

   1. Select **AB2D Application Admin**

   1. Select **Short-term Access Keys**

   1. Wait for access keys to generate

   1. Select the gray box under the "Option 1" section to copy the export variables to the clipboard

   1. Return to the terminal that you opened

   1. Paste the contents of the clipboard to the terminal

   1. Press "return" on the keyboard

1. Change to your "ab2d" repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```
   
1. Enter the following in the terminal to start the database tunnel

   ```ShellSession
   $ LOCAL_DB_PORT=1234 \
     && CONTROLLER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text) \
     &&  DATABASE_SECRET_DATETIME="2020-01-02-09-15-01" \
     && DATABASE_HOST=$(./Deploy/python3/get-database-secret.py ab2d-east-prod database_host $DATABASE_SECRET_DATETIME) \
     && ssh -N -L \
     "${LOCAL_DB_PORT}:${DATABASE_HOST}:5432" \
     ec2-user@"${CONTROLLER_PRIVATE_IP}" \
     -i "~/.ssh/ab2d-east-prod.pem"
   ```

1. Open a pgAdmin client in a Chrome tab by doing the following

   1. Select the elephant icon in the top right system bar

   1. Select **New pgAdmin 4 window**
