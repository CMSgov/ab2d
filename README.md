# AB2D

[![Build Status](https://travis-ci.org/CMSgov/ab2d.svg?branch=master)](https://travis-ci.org/CMSgov/ab2d)
[![Maintainability](https://api.codeclimate.com/v1/badges/a99a88d28ad37a79dbf6/maintainability)](https://codeclimate.com/github/codeclimate/codeclimate/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/a99a88d28ad37a79dbf6/test_coverage)](https://codeclimate.com/github/codeclimate/codeclimate/test_coverage)

## Table of Contents

1. [Clone the repo](#clone-the-repo)
1. [Configure local repo with "git-secrets" protection](#configure-local-repo-with-git-secrets-protection)
1. [Running in Docker](#running-in-docker)
1. [Deploying the solution](#deploying-the-solution)

## Clone the repo

1. Change to the code directory

   *Format:*
   
   ```ShellSession
   $ cd {code directory}
   ```

   *Example:*
   
   ```ShellSession
   $ cd ~/code
   ```

1. Clone the repo

   *Option #1: https clone:*
   
   ```ShellSession
   $ git clone https://github.com/CMSgov/ab2d.git
   ```

   *Option #2: ssh clone:*
   
   ```ShellSession
   $ git clone git@github.com:CMSgov/ab2d.git
   ```

## Configure local repo with "git-secrets" protection

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
   $ git secrets --install
   ```

1. Note the output

   ```
   Installed commit-msg hook to .git/hooks/commit-msg
   Installed pre-commit hook to .git/hooks/pre-commit
   Installed prepare-commit-msg hook to .git/hooks/prepare-commit-msg
   ```

1. Register AWS

   ```ShellSession
   $ git secrets --register-aws
   ```

1. See the README.md for "git-secrets" for more info

   > https://github.com/awslabs/git-secrets

## Running in Docker

1. Make the docker build

   ```ShellSession
   $ make docker-build
   ```

1. Start the container

   ```ShellSession
   $ docker-compose up
   ```

1. Note that this starts up Postgres, and both API & Worker Spring Boot apps

## Deploying the solution

*Jump to the following document:*

[Deploying the solution](Deploy/README.md)