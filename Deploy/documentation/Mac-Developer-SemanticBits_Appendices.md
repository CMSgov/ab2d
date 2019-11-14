# Mac Developer SemanticBits Appendices

## Table of Contents

1. [Appendix A: PostgrSQL 11](#appendix-a-postgresql-11)
   * [Install PostgreSQL 11](#install-postgresql-11)
   * [Uninstall PostgreSQL 11](#uninstall-postgresql-11)

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
