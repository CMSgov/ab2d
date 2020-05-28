# AB2D Deployment Implementation

## Table of Contents

1. [Obtain and import impl.ab2d.cms.gov common certificate](#obtain-and-import-implab2dcmsgov-common-certificate)
1. [Deploy to implementation](#deploy-to-implementation)
   * [Initialize or verify base environment](#initialize-or-verify-base-environment)
   
## Obtain and import impl.ab2d.cms.gov common certificate](#obtain-and-import-implab2dcmsgov-common-certificate)

> *** TO DO ***

## Initialize or verify base environment

1. Ensure that you are connected to CMS Cisco VPN
   
1. Initialize or verify environment

   ```ShellShession
   $ cd ~/code/ab2d/Deploy \
     && export CMS_ENV_PARAM=ab2d-east-impl \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export REGION_PARAM=us-east-1 \
     && export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && export CLOUD_TAMER_PARAM=true \
     && ./bash/initialize-environment.sh
   ```
