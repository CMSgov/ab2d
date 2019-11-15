# Mac Developer SemanticBits Appendices

## Table of Contents

1. [Appendix A: PostgrSQL 11](#appendix-a-postgresql-11)
   * [Install PostgreSQL 11](#install-postgresql-11)
   * [Uninstall PostgreSQL 11](#uninstall-postgresql-11)
1. [Appendix B: Create a Jira ticket for CMS VPN ticket access](#appendix-b-create-a-jira-ticket-for-cms-vpn-ticket-access)

## Appendix A: PostgrSQL 11

### Install PostgreSQL 11

1. Install PostgreSQL 11

   ```ShellSession
   $ brew install postgresql@11
   ```

1. Note the following caveats

   ```
   ==> Caveats
   To migrate existing data from a previous major version of PostgreSQL run:
     brew postgresql-upgrade-database

   To have launchd start postgresql now and restart at login:
     brew services start postgresql
   Or, if you don't want/need a background service you can just run:
     pg_ctl -D /usr/local/var/postgres start
   ```

1. Start PostgreSQL service

   ```ShellSession
   $ brew services start postgresql
   ```

1. Ensure that PostgreSQL is running on port 5432

   ```ShellSession
   $ netstat -an | grep 5432
   ```
   
### Uninstall PostgreSQL 11

1. Stop PostgreSQL service

   ```ShellSession
   $ brew services stop postgresql
   ```

1. Install PostgreSQL 11

   ```ShellSession
   $ brew uninstall postgresql@11
   ```

1. Ensure that PostgreSQL is no longer running on port 5432

   ```ShellSession
   $ netstat -an | grep 5432
   ```

## Appendix B: Create a Jira ticket for CMS VPN ticket access

1. Open Chrome

1. Enter the following in the address bar

   > https://jira.cms.gov/projects/CMSAWSOPS/issues/CMSAWSOPS-49590?filter=allopenissues

1. Select **Create**

1. Fill out the form as follows

   **Issue Type:** Access

   **Summary:** AWS VPN access for {your eua id}

   **Project Name:** Project 012 BlueButton

   **Account Alias:**  aws-hhs-cms-oeda-bfd

   **Types of Access/Resets:** Cisco AnyConnect Access

   **Severity:** Minimal

   **Urgency:** Medium

   **Description:**

   ```
   I'm an engineer working on the {your project name} project at CMS. Can I have AWS VPN access?

   User ID: {your eua id}

   Cellphone: {your cellphone number}

   Full name: {your first and last name}

   Email: {your semanticbits email}

   Thanks,

   {your first name}
   ```

   **Reported Source:** Self Service

   **Requested Due Date:** {3 business days from today's date}

1. Select **Create**

1. Verify that you receive an email regarding the issue
