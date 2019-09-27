# ab2d

## Table of Contents

1. [Read First Before Cloning](#read-first-before-cloning)

## Read First Before Cloning

1. Note that this is a public repo

1. Note that the following instructions will help prevent accidental check-ins of secrets

1. Note that these instructions assume that you have python3 and pip3 installed

1. Install the "detect-secrets" pip module

   ```ShellSession
   $ pip3 install detect-secrets
   ```

1. Install the "pre-commit" pip module

   ```ShellSession
   $ pip3 install pre-commit
   ```

1. Create code directory

   *Format:*
   
   ```ShellSession
   $ mkdir {code directory}
   ```

   *Example:*
   
   ```ShellSession
   $ mkdir ~/code
   ```

1. Clone the repo

   ```ShellSession
   $ git clone git@github.com:cmsgov/ab2d.git
   ```

1. Change to the repo directory

1. Set secrets baseline

   ```ShellSession
   $ detect-secrets scan > .secrets.baseline
   ```

1. Configure "pre-commit"

   1. Open ".pre-commit-config.yaml"

      ```ShellSession
      $ vim ~/.pre-commit-config.yaml
      ```

   1. Add the following to the file

      ```
      repos:
      - repo: https://github.com/CMSgov/ab2d
        rev: v1.0.0
        hooks:
        - id: detect-secrets
      ```

1. Before checking in code, you can do the following:

   1. Note that "untracked files" will not be checked for secrets
   
   1. Ensure that any untracked files are "git added" first before running the scan
   
   1. Scan for secrets

      ```ShellSession
      $ detect-secrets scan
      ```

   1. Verify that the "results" attribute of the JSON output looks like this

      ```
      "results": {},
      ```

