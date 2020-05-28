# AB2D Deployment (Greenfield)

## Table of Contents

* [Initialize or verify greenfield environment](#initialize-or-verify-greenfield-environment)

## Initialize or verify greenfield environment

1. Ensure that you are connected to CMS Cisco VPN

1. Initialize or verify environment

   ```ShellShession
   $ cd ~/code/ab2d/Deploy \
     && export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && ./bash/initialize-greenfield-environment.sh
   ```
