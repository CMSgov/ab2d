# AB2D

[![Build Status](https://travis-ci.org/CMSgov/ab2d.svg?branch=master)](https://travis-ci.org/CMSgov/ab2d)
[![Maintainability](https://api.codeclimate.com/v1/badges/322dab715b4324c33fee/maintainability)](https://codeclimate.com/github/CMSgov/ab2d/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/322dab715b4324c33fee/test_coverage)](https://codeclimate.com/github/CMSgov/ab2d/test_coverage)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/CMSgov/ab2d.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/CMSgov/ab2d/alerts/)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/CMSgov/ab2d.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/CMSgov/ab2d/context:java)
[![Language grade: Python](https://img.shields.io/lgtm/grade/python/g/CMSgov/ab2d.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/CMSgov/ab2d/context:python)

## Table of Contents

1. [Configure local repo with "git-secrets" protection](#configure-local-repo-with-git-secrets-protection)
1. [Configure New Relic](#configure-new-relic)
1. [Configure AWS CLI](#configure-aws-cli)
1. [Create volume directory](#create-volume-directory)
1. [Running in Docker](#running-in-docker)
1. [Deploying the solution](#deploying-the-solution)

## Configure local repo with "git-secrets" protection

*This must be done each time you clone a new copy of the repo*

1. Install git-secrets (on MacOS with brew simply run: `brew install git-secrets`)

    - See the README.md for "git-secrets" for more info or to install on other platforms:

       > https://github.com/awslabs/git-secrets

1. Change to the repo directory

   *Format:*
   
   ```ShellSession
   $ cd {code directory}/ab2d
   ```

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Install "git-secrets" for the repo

   ```ShellSession
   $ git-secrets --install
   ```

1. Note the output

   ```
   Installed commit-msg hook to .git/hooks/commit-msg
   Installed pre-commit hook to .git/hooks/pre-commit
   Installed prepare-commit-msg hook to .git/hooks/prepare-commit-msg
   ```

1. Register AWS

   ```ShellSession
   $ git-secrets --register-aws
   ```

## Configure New Relic

1. If you do not have a New Relic account, jump to the following section:

   [Configure AWS CLI](#configure-aws-cli)

1. Configure New Relic environment variables

   1. Get and note the "AB2D New Relic license key" from the "ab2d" vault in 1Password

   1. Backup the file that you use for setting up your shell's environment

      *Example for bash shell:*

      ```ShellSession
      $ cp ~/.bash_profile ~/.bash_profile_backup
      ```

   1. Add section heading for New Relic environment variables

      *Example for bash shell:*
      
      ```ShellSession
      $ printf '\n# Set New Relic environment variables' >> ~/.bash_profile
      ```

   1. Configure the New Relic application name for development

      *Example using your username as part of application name for bash shell:*

      ```ShellSession
      $ printf "\nexport NEW_RELIC_APP_NAME='AB2D for $USER'" >> ~/.bash_profile
      ```

   1. Configure the New Relic license key for development

      *NOTE: Be sure to change "{your new relic license key}" below to the key that you retreived from 1Password.*

      *Format example for bash shell:*

      ```ShellSession
      $ printf "\nexport NEW_RELIC_LICENSE_KEY='{your new relic license key}'" >> ~/.bash_profile
      ```

   1. Apply changes to current terminal session

      *Example for bash shell:*

      ```ShellSession
      $ source ~/.bash_profile
      ```

## Configure AWS CLI

1. Note that before proceeding, you will need to have a user with AWS programming credentials that has at least the following permissions

   ```
   {
       "Effect": "Allow",
       "Action": [
           "s3:Get*",
           "s3:List*"
       ],
       "Resource": "*"
   } 
   ```

1. Determine if you already have the AWS CLI installed

   ```ShellSession
   $ aws --version
   ```

1. If you are doing development within an IDE, ensure that you have one of these three options in place:

   *Option #1: Verify a default profile is in place*

   ```ShellSession
   $ cat ~/.aws/credentials
   ```

   *Option #2: Set AWS profile environment variable to desired profile within the API and/or Worker Run/Debug configurations:*

   - AWS_PROFILE={desired profile from '~/.aws/credentials' file}

   *Option #3: Set AWS credential environment variables within the API and/or Worker Run/Debug configurations:*

   - AWS_ACCESS_KEY_ID={desired AWS access key}

   - AWS_SECRET_ACCESS_KEY={AWS secret access key that corresponds to the desired AWS access key}

1. If you are running a jar file from the terminal, ensure that you have one of these three options in place:

   *Option #1: Verify a default profile is in place*

   ```ShellSession
   $ cat ~/.aws/credentials
   ```

   *Option #2: Set AWS_PROFILE environment variable to desired profile:*

   ```ShellSession
   $ export AWS_PROFILE={desired profile from '~/.aws/credentials' file}
   ```

   *Option #3: Set AWS credential environment variables:*

   ```ShellSession
   $ export AWS_ACCESS_KEY_ID={desired AWS access key}
   $ export AWS_SECRET_ACCESS_KEY={desired AWS access key}
   ```

1. If you already have the AWS CLI installed and configured, jump to the following section:

   [Running in Docker](#running-in-docker)

1. Determine if you have python3 installed

   ```ShellSession
   $ python3 --version
   ```

1. If you don't have python3 installed, do the following:

   1. Ensure that you have ownership of homebrew
   
      ```ShellSession
      $ sudo chown -R $(whoami) \
        /usr/local/var/homebrew \
        /usr/local/homebrew
      ```
   
   1. Ensure that you have ownership of dependent directories
   
      ```ShellSession
      $ sudo chown -R $(whoami) \
        /usr/local/etc/bash_completion.d \
        /usr/local/lib/pkgconfig \
        /usr/local/share/aclocal \
        /usr/local/share/doc \
        /usr/local/share/man \
        /usr/local/share/man/man1 \
        /usr/local/share/man/man8 \
        /usr/local/share/zsh \
        /usr/local/share/zsh/site-functions
      ```

   1. If python3 is not installed, do the following:

      ```ShellSession
      $ brew install python3
      ```

   1. Upgrade pip3

      *Ignore any incompatible warnings.*
   
      ```ShellSession
      $ pip3 install --upgrade pip
      ```

1. Install the AWS CLI

   ```ShellSession
   $ pip3 install awscli --upgrade --user --no-warn-script-location
   ```

1. Set up your shell environment script with the path to the AWS CLI

   1. Back up your current shell environment script
   
      *Example for bash:*

      ```ShellSession
      $ cp ~/.bash_profile ~/.bash_profile_backup
      ```
      
   1. Add a comment heading for setting the AWS CLI path
      
      ```ShellSession
      $ printf '\n# Add AWS CLI to Path' >> ~/.bash_profile
      ```

   1. Add the AWS CLI path

      ```ShellSession
      $ printf '\nexport PATH="$PATH:$HOME/Library/Python/3.7/bin"' >> ~/.bash_profile
      ```

   1. Apply the changes to the current terminal session

      *Example for bash:*
      
      ```ShellSession
      $ source ~/.bash_profile
      ```

   1. Verify that AWS CLI is working by verifying its version
   
      ```ShellSession
      $ aws --version
      ```

1. Configure your default AWS CLI profile

   ```ShellSession
   $ aws configure
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

## Create volume directory

1. Create a volume directory

   ```ShellSession
   $ mkdir -p /tmp/ab2d_efs_mount
   ```

1. Note that this directory acts as a shared volume for the API and worker containers

## Running in Docker

1. Make the docker build

   ```ShellSession
   $ make docker-build
   ```

1. Start the container

   ```ShellSession
   $ docker-compose up --build
   ```

1. Note that this starts up Postgres, and both API & Worker Spring Boot apps

## Deploying the solution

1. Note that the "AB2D Deploy" document includes the following

   - required setup of the development machine (Mac specific)

   - a 'deploy the solution' section that directs you to an environment specific document

1. Jump to the "AB2D Deploy" document

   [AB2D Deploy](Deploy/README.md)
