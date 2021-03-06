# AB2D

[![Maintainability](https://api.codeclimate.com/v1/badges/322dab715b4324c33fee/maintainability)](https://codeclimate.com/github/CMSgov/ab2d/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/322dab715b4324c33fee/test_coverage)](https://codeclimate.com/github/CMSgov/ab2d/test_coverage)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/CMSgov/ab2d.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/CMSgov/ab2d/alerts/)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/CMSgov/ab2d.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/CMSgov/ab2d/context:java)
[![Language grade: Python](https://img.shields.io/lgtm/grade/python/g/CMSgov/ab2d.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/CMSgov/ab2d/context:python)
[![Automated Release Notes by gren](https://img.shields.io/badge/%F0%9F%A4%96-release%20notes-00B2EE.svg)](https://github-tools.github.io/github-release-notes/)

## Table of Contents

1. [Configure local repo with "git-secrets" protection](#configure-local-repo-with-git-secrets-protection)
1. [Configure New Relic](#configure-new-relic)
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

   [Create volume directory](#create-volume-directory)

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

## Create volume directory

1. Create a volume directory

   ```ShellSession
   $ mkdir -p /opt/ab2d
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
