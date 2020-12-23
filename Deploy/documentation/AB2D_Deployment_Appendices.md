# AB2D Deployment Appendices

## Table of Contents

1. [Appendix A: Access the CMS AWS console](#appendix-a-access-the-cms-aws-console)
1. [Appendix B: View the current gold disk images](#appendix-b-view-the-current-gold-disk-images)
1. [Appendix C: Interact with the developer S3 bucket](#appendix-c-interact-with-the-developer-s3-bucket)
1. [Appendix D: Delete and recreate IAM instance profiles, roles, and policies](#appendix-d-delete-and-recreate-iam-instance-profiles-roles-and-policies)
   * [Delete instance profiles](#delete-instance-profiles)
   * [Delete roles](#delete-roles)
   * [Delete policies not used by IAM users](#delete-policies-not-used-by-iam-users)
1. [Appendix E: Interacting with IAM policy versions](#appendix-e-interacting-with-iam-policy-versions)
1. [Appendix F: Interacting with Elastic Container Repository](#appendix-f-interacting-with-elastic-container-repository)
1. [Appendix G: Generate and test the AB2D website](#appendix-g-generate-and-test-the-ab2d-website)
1. [Appendix H: Get log file from a node](#appendix-h-get-log-file-from-a-node)
1. [Appendix I: Connect to a running container](#appendix-i-connect-to-a-running-container)
1. [Appendix J: Delete and recreate database](#appendix-j-delete-and-recreate-database)
1. [Appendix K: Complete DevOps linting checks](#appendix-k-complete-devops-linting-checks)
   * [Complete terraform linting](#complete-terraform-linting)
   * [Complete python linting](#complete-python-linting)
1. [Appendix L: View existing EUA job codes](#appendix-l-view-existing-eua-job-codes)
1. [Appendix M: Make AB2D static website unavailable](#appendix-m-make-ab2d-static-website-unavailable)
   * [Delete the contents of the websites S3 bucket](#delete-the-contents-of-the-websites-s3-bucket)
   * [Delete the cached website files from the CloudFront edge caches before they expire](#delete-the-cached-website-files-from-the-cloudfront-edge-caches-before-they-expire)
1. [Appendix N: Destroy and redploy API and Worker nodes](#appendix-n-destroy-and-redploy-api-and-worker-nodes)
1. [Appendix O: Destroy complete environment](#appendix-o-destroy-complete-environment)
1. [Appendix P: Display disk space](#appendix-p-display-disk-space)
1. [Appendix Q: Test API using swagger](#appendix-q-test-api-using-swagger)
1. [Appendix R: Update userdata for auto scaling groups through the AWS console](#appendix-r-update-userdata-for-auto-scaling-groups-through-the-aws-console)
1. [Appendix S: Install Ruby on RedHat linux](#appendix-s-install-ruby-on-redhat-linux)
1. [Appendix T: Test getting and decrypting a file from S3](#appendix-t-test-getting-and-decrypting-a-file-from-s3)
1. [Appendix U: Interact with the New Relic infrastructure agent](#appendix-u-interact-with-the-new-relic-infrastructure-agent)
1. [Appendix V: Add a new environment variable for ECS docker containers](#appendix-v-add-a-new-environment-variable-for-ecs-docker-containers)
1. [Appendix W: Launch a base EC2 instance that is created from gold disk AMI](#appendix-w-launch-a-base-ec2-instance-that-is-created-from-gold-disk-ami)
1. [Appendix X: Verify access to the opt-out S3 bucket from sandbox worker nodes](#appendix-x-verify-access-to-the-opt-out-s3-bucket-from-sandbox-worker-nodes)
   * [Test getting a public S3 file using AWS CLI and no sign request](#test-getting-a-public-s3-file-using-aws-cli-and-no-sign-request)
   * [Test downloading a public S3 file using the AWS CLI without credentials](#test-downloading-a-public-s3-file-using-the-aws-cli-without-credentials)
   * [Test downloading a public S3 file using the AWS CLI with the "ab2d-s3-signing" profile](#test-downloading-a-public-s3-file-using-the-aws-cli-with-the-ab2d-s3-signing-profile)
   * [Test interacting with a public S3 file using the AWS Java SDK with environment variables](#test-interacting-with-a-public-s3-file-using-the-aws-java-sdk-with-environment-variables)
   * [Test interacting with a public S3 file on a worker node](#test-interacting-with-a-public-s3-file-on-a-worker-node)
1. [Appendix Y: Test the opt-out process using IntelliJ](#appendix-y-test-the-opt-out-process-using-intellij)
1. [Appendix Z: Test configuration of JVM settings within a container](#appendix-z-test-configuration-of-jvm-settings-within-a-container)
   * [Check the default "heapsize" and "maxram" settings for an api node](#check-the-default-heapsize-and-maxram-settings-for-an-api-node)
   * [Verify JVM parameters for an api node](#verify-jvm-parameters-for-an-api-node)
   * [Check the default "heapsize" and "maxram" settings for a worker node](#check-the-default-heapsize-and-maxram-settings-for-a-worker-node)
   * [Verify JVM parameters for a worker node](#verify-jvm-parameters-for-a-worker-node)
1. [Appendix AA: View CCS Cloud VPN Public IPs](#appendix-aa-view-ccs-cloud-vpn-public-ips)
1. [Appendix BB: Update controller](#appendix-bb-update-controller)
1. [Appendix CC: Fix bad terraform component](#appendix-cc-fix-bad-terraform-component)
1. [Appendix DD: Test running development automation from Jenkins master](#appendix-dd-test-running-development-automation-from-jenkins-master)
1. [Appendix EE: Fix Jenkins reverse proxy error](#appendix-ee-fix-jenkins-reverse-proxy-error)
1. [Appendix FF: Manually add JDK 13 to a node that already has JDK 8](#appendix-ff-manually-add-jdk-13-to-a-node-that-already-has-jdk8)
1. [Appendix GG: Destroy Jenkins agent](#appendix-gg-destroy-jenkins-agent)
1. [Appendix HH: Manual test of Splunk configuration](#appendix-hh-manual-test-of-splunk-configuration)
   * [Verify or create a "cwlg_lambda_splunk_hec_role" role](#verify-or-create-a-cwlglambdasplunkhecrole-role)
   * [Prepare IAM policies and roles for CloudWatch Log groups](#prepare-iam-policies-and-roles-for-cloudwatch-log-groups)
   * [Configure CloudWatch Log groups](#configure-cloudwatch-log-groups)
   * [Configure CloudWatch Log groups for management environment](#configure-cloudwatch-log-groups-for-management-environment)
     * [Configure CloudTrail CloudWatch Log group for management environment](#configure-cloudtrail-cloudwatch-log-group-for-management-environment)
     * [Configure VPC flow log CloudWatch Log group for management environment](#configure-vpc-flow-log-cloudwatch-log-group-for-management-environment)
     * [Onboard first Jenkins master log to CloudWatch Log groups](#onboard-first-jenkins-master-log-to-cloudwatch-log-groups)
     * [Onboard additional CloudWatch log groups for Jenkins master](#onboard-additional-cloudwatch-log-groups-for-jenkins-master)
     * [Onboard first Jenkins agent log to CloudWatch Log groups](#onboard-first-jenkins-agent-log-to-cloudwatch-log-groups)
     * [Onboard additional CloudWatch log groups for Jenkins agent](#onboard-additional-cloudwatch-log-groups-for-jenkins-agent)
     * [Verify logging to CloudWatch Log Group for management environment](#verify-logging-to-cloudwatch-log-group-for-management-environment)
   * [Configure CloudWatch Log groups for development environment](#configure-cloudwatch-log-groups-for-development-environment)
     * [Configure CloudTrail CloudWatch Log group for development environment](#configure-cloudtrail-cloudwatch-log-group-for-development-environment)
     * [Configure VPC flow log CloudWatch Log group for development environment](#configure-vpc-flow-log-cloudwatch-log-group-for-development-environment)
     * [Onboard first deployment controller log to CloudWatch Log groups for development environment](#onboard-first-deployment-controller-log-to-cloudwatch-log-groups-for-development-environment)
     * [Onboard additional CloudWatch log groups for deployment controller log for development environment](#onboard-additional-cloudwatch-log-groups-for-deployment-controller-log-for-development-environment)
     * [Onboard first api node log to CloudWatch Log groups for development environment](#onboard-first-api-node-log-to-cloudwatch-log-groups-for-development-environment)
     * [Onboard additional CloudWatch log groups for first api node log for development environment](#onboard-additional-cloudwatch-log-groups-for-first-api-node-log-for-development-environment)
   * [Configure CloudWatch Log groups for sandbox environment](#configure-cloudwatch-log-groups-for-sandbox-environment)
     * [Configure CloudTrail CloudWatch Log group for sandbox environment](#configure-cloudtrail-cloudwatch-log-group-for-sandbox-environment)
     * [Configure VPC flow log CloudWatch Log group for sandbox environment](#configure-vpc-flow-log-cloudwatch-log-group-for-sandbox-environment)
     * [Onboard first deployment controller log to CloudWatch Log groups for sandbox environment](#onboard-first-deployment-controller-log-to-cloudwatch-log-groups-for-sandbox-environment)
     * [Onboard additional CloudWatch log groups for deployment controller log for sandbox environment](#onboard-additional-cloudwatch-log-groups-for-deployment-controller-log-for-sandbox-environment)
     * [Onboard first api node log to CloudWatch Log groups for sandbox environment](#onboard-first-api-node-log-to-cloudwatch-log-groups-for-sandbox-environment)
     * [Onboard additional CloudWatch log groups for first api node log for sandbox environment](#onboard-additional-cloudwatch-log-groups-for-first-api-node-log-for-sandbox-environment)
   * [Configure CloudWatch Log groups for impl environment](#configure-cloudwatch-log-groups-for-impl-environment)
     * [Configure CloudTrail CloudWatch Log group for impl environment](#configure-cloudtrail-cloudwatch-log-group-for-impl-environment)
     * [Configure VPC flow log CloudWatch Log group for impl environment](#configure-vpc-flow-log-cloudwatch-log-group-for-impl-environment)
     * [Onboard first deployment controller log to CloudWatch Log groups for impl environment](#onboard-first-deployment-controller-log-to-cloudwatch-log-groups-for-impl-environment)
     * [Onboard additional CloudWatch log groups for deployment controller log for impl environment](#onboard-additional-cloudwatch-log-groups-for-deployment-controller-log-for-impl-environment)
     * [Onboard first api node log to CloudWatch Log groups for impl environment](#onboard-first-api-node-log-to-cloudwatch-log-groups-for-impl-environment)
     * [Onboard additional CloudWatch log groups for first api node log for impl environment](#onboard-additional-cloudwatch-log-groups-for-first-api-node-log-for-impl-environment)
1. [Appendix II: Get application load balancer access logs](#appendix-ii-get-application-load-balancer-access-logs)
1. [Appendix JJ: Export CloudWatch Log Group data to S3](#appendix-jj-export-cloudwatch-log-group-data-to-s3)
1. [Appendix KK: Change the BFD certificate in AB2D keystores](#appendix-kk-change-the-bfd-certificate-in-ab2d-keystores)
1. [Appendix LL: Update existing WAF](#appendix-ll-update-existing-waf)
1. [Appendix MM: Create new AMI from latest gold disk image](#appendix-mm-create-new-ami-from-latest-gold-disk-image)
1. [Appendix NN: Manually test the deployment](#appendix-nn-manually-test-the-deployment)
   * [Manually test the deployment for sandbox](#manually-test-the-deployment-for-sandbox)
   * [Manually test the deployment for production](#manually-test-the-deployment-for-production)
1. [Appendix OO: Merge a specific commit from master into your branch](#appendix-oo-merge-a-specific-commit-from-master-into-your-branch)
1. [Appendix PP: Test running development automation from development machine](#appendix-pp-test-running-development-automation-from-development-machine)
1. [Appendix QQ: Set up demonstration of cross account access of an encrypted S3 bucket](#appendix-qq-set-up-demonstration-of-cross-account-access-of-an-encrypted-s3-bucket)
1. [Appendix RR: Tealium and Google Analytics notes](#appendix-rr-tealium-and-google-analytics-notes)
1. [Appendix SS: Destroy API and Worker clusters](#appendix-ss-destroy-api-and-worker-clusters)
1. [Appendix TT: Migrate terraform state from shared environment to main environment](#appendix-tt-migrate-terraform-state-from-shared-environment-to-main-environment)
1. [Appendix UU: Access Health Plan Management System (HPMS)](#appendix-uu-access-health-plan-management-system-hpms)
1. [Appendix VV: Import an existing resource using terraform](#appendix-vv-import-an-existing-resource-using-terraform)
   * [Import an existing IAM role](#import-an-existing-iam-role)
   * [Import an existing IAM Instance Profile](#import-an-existing-iam-instance-profile)
   * [Import an existing KMS key](#import-an-existing-kms-key)
1. [Appendix WW: Use an SSH tunnel to query production database from local machine](#appendix-ww-use-an-ssh-tunnel-to-query-production-database-from-local-machine)
1. [Appendix XX: Create a self-signed certificate for an EC2 load balancer](#appendix-xx-create-a-self-signed-certificate-for-an-ec2-load-balancer)
1. [Appendix YY: Review VictorOps documentation](#appendix-yy-review-victorops-documentation)
   * [VictorOps Sources](#victorops-sources)
   * [VictorOps Overview](#victorops-overview)
1. [Appendix AAA: Upload static website to an Akamai Upload Directory within Akamai NetStorage](#appendix-aaa-upload-static-website-to-an-akamai-upload-directory-within-akamai-netstorage)
1. [Appendix BBB: Delete all files in an Akamai Upload Directory within Akamai NetStorage](#appendix-zz-delete-all-files-in-an-akamai-upload-directory-within-akamai-netstorage)
1. [Appendix CCC: Reconcile terraform state between two environments](#appendix-ccc-reconcile-terraform-state-between-two-environments)
   * [Reconcile terraform state of development environment with terraform state of implementation environment](#reconcile-terraform-state-of-development-environment-with-terraform-state-of-implementation-environment)
   * [Reconcile terraform state of sandbox environment with terraform state of implementation environment](#reconcile-terraform-state-of-sandbox-environment-with-terraform-state-of-implementation-environment)
1. [Appendix DDD: Backup and recovery](#appendix-ddd-backup-and-recovery)
1. [Appendix EEE: Modify the database instance type](#appendix-eee-modify-the-database-instance-type)
1. [Appendix FFF: Run e2e tests](#appendix-fff-run-e2e-tests)
   * [Run e2e tests for development](#run-e2e-tests-for-development)
   * [Run e2e tests for sandbox](#run-e2e-tests-for-sandbox)
   * [Run e2e tests for production](#run-e2e-tests-for-production)
1. [Appendix GGG: Retrieve a copy of remote terraform state file for review](#appendix-ggg-retrieve-a-copy-of-remote-terraform-state-file-for-review)
1. [Appendix HHH: Manually change a tag on controller and update its terraform state](#appendix-hhh-manually-change-a-tag-on-controller-and-update-its-terraform-state)
1. [Appendix III: Issue a schedule override in VictorOps](#appendix-iii-issue-a-schedule-override-in-victorops)
1. [Appendix JJJ: Change the Jenkins home directory on the Jenkins agent](#appendix-jjj-change-the-jenkins-home-directory-on-the-jenkins-agent)
1. [Appendix KKK: Change MFA to Google Authenticator for accessing Jira](#appendix-kkk-change-mfa-to-google-authenticator-for-accessing-jira)
1. [Appendix LLL: Add a volume to jenkins agent and extend the log volume to use it](#appendix-lll-add-a-volume-to-jenkins-agent-and-extend-the-log-volume-to-use-it)
1. [Appendix MMM: Upgrade Jenkins Agent from AWS CLI 1 to AWS CLI 2](#appendix-mmm-upgrade-jenkins-agent-from-aws-cli-1-to-aws-cli-2)
   * [Uninstall AWS CLI 1 using pip](#uninstall-aws-cli-1-using-pip)
   * [Install and verify AWS CLI 2](#install-and-verify-aws-cli-2)
1. [Appendix NNN: Manually install Chef Inspec on existing Jenkins Agent](#appendix-nnn-manually-install-chef-inspec-on-existing-jenkins-agent)
1. [Appendix OOO: Connect to Jenkins agent through the Jenkins master using the ProxyJump flag](#appendix-ooo-connect-to-jenkins-agent-through-the-jenkins-master-using-the-proxyjump-flag)
1. [Appendix PPP: Create and work with CSV database backup](#appendix-ppp-create-and-work-with-csv-database-backup)
   * [Create and retrieve CSV database backup](#create-and-retrieve-csv-database-backup)
   * [Create a second schema that uses CSV database backup](#create-a-second-schema-that-uses-csv-database-backup)
   * [Reconcile backup data with current data](#reconcile-backup-data-with-current-data)
1. [Appendix QQQ: Get private IP address](#appendix-qqq-get-private-ip-address)
1. [Appendix RRR: Protect the existing RDS database using AWS CLI](#appendix-rrr-protect-the-existing-rds-database-using-aws-cli)
1. [Appendix SSS: Review RDS reserved instance utilization from AWS console](#appendix-sss-review-rds-reserved-instance-utilization-from-aws-console)
1. [Appendix TTT: Reset master to a specific commit](#appendix-ttt-reset-master-to-a-specific-commit)
   * [Force push to master](#force-push-to-master)
   * [Reconcile your master branch with a remote branch that was force pushed by someone else](#reconcile-your-master-branch-with-a-remote-branch-that-was-force-pushed-by-someone-else)
   * [Rebase an existing branch to reconcile it with a master that has been reset](#rebase-an-existing-branch-to-reconcile-it-with-a-master-that-has-been-reset)
1. [Appendix UUU: Migrate VictorOps-Slack integration from a real slack user to slack service user](#appendix-uuu-migrate-victorops-slack-integration-from-a-real-slack-user-to-slack-service-user)
1. [Appendix VVV: Add a volume to jenkins agent and extend the root volume to use it](#appendix-vvv-add-a-volume-to-jenkins-agent-and-extend-the-root-volume-to-use-it)
1. [Appendix WWW: Whitelist IP addresses in Akamai for Prod](#whitelist-ip-addresses-in-akamai-for-prod)
1. [Appendix XXX: Fix CloudTamer scripts broken by ITOPS role change](#appendix-xxx-fix-cloudtamer-scripts-broken-by-itops-role-change)
1. [Appendix YYY: Add IAM components under the new ITOPS restrictions](#appendix-yyy-add-iam-components-under-the-new-itops-restrictions)
1. [Appendix ZZZ: Revert your branch to a previous commit and force push to GitHub](#appendix-zzz-revert-your-branch-to-a-previous-commit-and-force-push-to-github)
1. [Appendix AAAA: Change encryption key of AWS RDS Instance](#appendix-aaaa-change-encryption-key-of-aws-rds-instance)
   * [Create manual snapshot of RDS DB instance](#create-manual-snapshot-of-rds-db-instance)
   * [Copy manual snapshot using desired KMS key](#restore-manual-snapshot-using-desired-kms-key
   * [Create new RDS DB instance from the snapshot copy](#create-new-rds-db-instance-from-the-snapshot-copy)

## Appendix A: Access the CMS AWS console

1. Log into Cisco AnyConnect client

   1. Select **Launchpad**
   
   1. Select **Cisco AnyConnect Secure Mobility Client**
   
   1. If a "Cisco AnyConnect Secure Mobility Client is not optimized for your Mac and needs to be updated" message appears, do the following:
      
      1. Select **OK**
   
      1. See the following for more information
   
         > https://support.apple.com/en-us/HT208436
   
   1. Enter the following in the **VPN** dropdown
   
      ```
      cloudvpn.cms.gov
      ```
   
   1. Select **Connect**
   
   1. Enter the following on the "Please enter your username and password" window
   
      - **USERNAME:** {your eua id}
   
      - **PASSWORD:** {password for CloudTamer and Cloud VPN}
   
      - **2nd Password:** {latest 6 number from the google authentication app on mobile phone}
   
   1. Select **OK**
   
   1. Select **Accept**
   
   1. If a "vpndownloader is not optimized for your Mac and needs to be updated" message appears, do the following:
      
      1. Select **OK**
   
      1. See the following for more information
   
         > https://support.apple.com/en-us/HT208436
   
   1. Verify that the VPN successfully connects

1. Log on to CloudTamer

   1. Open Chrome

   1. Enter the following in the address bar

      > https://cloudtamer.cms.gov

   1. Enter the following on the CloudTamer logon page

      - **Username:** {your eua id}

      - **Password:** {password for CloudTamer and Cloud VPN}

      - **Dropdown:** CMS Cloud Services

   1. Select **Log In**

1. Select **Projects** from the leftmost panel

1. Select **Filters**

1. Type the following in the **By Keyword** text box

   ab2d

1. Select **Apply Filters**

1. Select **Account Access** beside "Project AB2D"

1. Select the account
   
   ```
   aws-cms-oit-iusg-acct66 (349849222861)
   ```

1. Select the cloud access role

   
   ```
   West-AB2D
   ```

1. Select **Web Access**

1. Note that the AWS console for the AB2D project should now appear

## Appendix B: View the current gold disk images

1. Open Chrome

1. Enter the following in the address bar

   > https://github.cms.gov/CCSVDC/gold-image/blob/master/ami_latest.txt

## Appendix C: Interact with the developer S3 bucket

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Verify that the "cms-ab2d-dev" bucket exists

   ```ShellSession
   $ aws --region us-east-1 s3api list-buckets \
     --query "Buckets[?Name=='cms-ab2d-dev'].Name" \
     --output text
   ```

1. Create a text file as an example (optional)

   ```ShellSession
   $ echo "test" > opt-out.txt
   ```
   
1. Put a file in the root of the bucket

   *Example using a file named opt-out.txt exists in the current directory:*
   
   ```ShellSession
   $ aws --region us-east-1 s3api put-object \
     --bucket cms-ab2d-dev \
     --key opt-out.txt \
     --body ./opt-out.txt
   ```

1. List objects in the root of the bucket

   ```ShellSession
   $ aws --region us-east-1 s3api list-objects \
     --bucket cms-ab2d-dev \
     --query 'Contents[].Key' \
     --output text
   ```

1. Get a file from the root of the bucket

   ```ShellSession
   $ aws --region us-east-1 s3api get-object \
     --bucket cms-ab2d-dev \
     --key opt-out.txt \
     ./opt-out-copy.txt
   ```

1. Delete a file from the root of the bucket

   ```ShellSession
   $ aws --region us-east-1 s3api delete-object \
     --bucket cms-ab2d-dev \
     --key opt-out.txt
   ```

1. Verify that the object is gone by listing objects in the root of the bucket again

   ```ShellSession
   $ aws --region us-east-1 s3api list-objects \
     --bucket cms-ab2d-dev \
     --query 'Contents[].Key' \
     --output text
   ```

## Appendix D: Delete and recreate IAM instance profiles, roles, and policies

### Delete instance profiles

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Detach "Ab2dInstanceRole" from "Ab2dInstanceProfile"

   ```ShellSession
   $ aws iam remove-role-from-instance-profile \
     --role-name Ab2dInstanceRole \
     --instance-profile-name Ab2dInstanceProfile
   ```

1. Delete instance profile

   ```ShellSession
   $ aws iam delete-instance-profile \
     --instance-profile-name Ab2dInstanceProfile
   ```

### Delete roles

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Detach policies from the "Ab2dManagedRole" role

   ```ShellSession
   $ aws iam detach-role-policy \
     --role-name Ab2dManagedRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dAccessPolicy
   ```

1. Delete "Ab2dManagedRole" role

   ```ShelSession
   $ aws iam delete-role \
     --role-name Ab2dManagedRole
   ```

1. Detach policies from the "Ab2dInstanceRole" role

   ```ShellSession
   $ aws iam detach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dAssumePolicy
   $ aws iam detach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dPackerPolicy
   $ aws iam detach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dS3AccessPolicy
   $ aws iam detach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role
   ```

1. Delete "Ab2dInstanceRole" role

   ```ShelSession
   $ aws iam delete-role \
     --role-name Ab2dInstanceRole
   ```

### Delete policies not used by IAM users

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Delete "Ab2dAccessPolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dAccessPolicy
   ```

1. Delete "Ab2dAssumePolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dAssumePolicy
   ```

1. Delete "Ab2dPackerPolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dPackerPolicy
   ```

1. Delete "Ab2dS3AccessPolicy"

   ```ShellSession
   $ aws iam delete-policy \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dS3AccessPolicy
   ```

## Appendix E: Interacting with IAM policy versions

1. List policy versions

   1. Enter the following
   
      *Example of listing policy versions of the "Ab2dAssumePolicy" policy:*
   
      ```ShellSession
      $ aws iam list-policy-versions \
        --policy-arn arn:aws:iam::114601554524:policy/Ab2dAssumePolicy
      ```

   1. Examine the output

      *Example of listing policy versions of the "Ab2dAssumePolicy" policy:*
      
      ```
      {
          "Versions": [
              {
                  "VersionId": "v2",
                  "IsDefaultVersion": true,
                  "CreateDate": "2019-10-11T23:23:54Z"
              },
              {
                  "VersionId": "v1",
                  "IsDefaultVersion": false,
                  "CreateDate": "2019-10-04T19:11:21Z"
              }
          ]
      }
      ```
   
1. Delete a policy version

   *Example of deleting "v1" of the "Ab2dAssumePolicy" policy:*

   ```ShellSession
   $ aws iam delete-policy-version \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dAssumePolicy \
     --version-id v1
   ```

## Appendix F: Interacting with Elastic Container Repository

1. List ECR repositories

   ```ShellSession
   $ aws --region us-east-1 ecr describe-repositories \
     --query "repositories[*].repositoryName" \
     --output text
   ```

1. Delete an ECR repository

   *Example deleting a repository named "ab2d_sbdemo-dev_api":*
   
   ```ShellSession
   $ aws --region us-east-1 ecr delete-repository \
     --repository-name ab2d_sbdemo-dev_api \
     --force
   ```

## Appendix G: Generate and test the AB2D website

1. Change to the "website" directory

   ```ShellSession
   $ cd ~/code/ab2d/website
   ```

1. Generate and test the website

   1. Ensure required gems are installed

      ```ShellSession
      $ bundle install
      ```

   1. Generate and serve website on the jekyll server

      ```ShellSession
      $ bundle exec jekyll serve
      ```
     
   1. Open Chrome

   1. Enter the following in the address bar
   
      > http://127.0.0.1:4000
      
   1. Verify that the website comes up

   1. Return to the terminal where the jekyll server is running
   
   1. Press **control+c** on the keyboard to stop the Jekyll server

1. Verify the generated site

   1. Note that a "_site" directory was automatically generated when you ran "bundle exec jekyll serve"
   
   1. List the contents of the directory

      ```ShellSession
      $ ls _site
      ```
    
   1. Note that the following two files are used as part of configuring the website within S3

      - index.html

      - 404.html

## Appendix H: Get log file from a node

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

1. Set target environment

   *Example for "Dev" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   ```

1. Set controller access variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export CONTROLLER_PUBLIC_IP=52.7.241.208
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ export CONTROLLER_PUBLIC_IP=10.242.36.48
   $ export SSH_USER_NAME=ec2-user
   ```

1. Copy the key to the controller

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}:~/.ssh
   ```
   
1. Connect to the controller

   *Format:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
   ```

1. Set node variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   $ export NODE_PRIVATE_IP=10.242.26.83
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment (api node 1):*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   $ export NODE_PRIVATE_IP=10.242.31.193
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment (api node 2):*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   $ export NODE_PRIVATE_IP=10.242.31.48
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment (worker node 1):*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   $ export NODE_PRIVATE_IP=10.242.31.8
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment (worker node 2):*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   $ export NODE_PRIVATE_IP=10.242.31.132
   $ export SSH_USER_NAME=ec2-user
   ```

1. Connect to a node

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${NODE_PRIVATE_IP}
   ```

1. Switch to root

   ```ShellSession
   $ sudo su
   ```

1. If you want to clear the log to get fresh logging, do the following:

   1. Clear the log

      ```ShellSession
      $ sudo cat /dev/null > /var/log/messages
      ```

   2. Wait a few minutes to get the fresh logging
   
1. Copy "messages" log to ec2-user home directory

   ```ShellSession
   $ cp /var/log/messages /home/ec2-user
   ```

1. Change the ownership on the file

   ```ShellSession
   $ chown ec2-user:ec2-user /home/ec2-user/messages
   ```

1. Exit the root user

   ```ShellSession
   $ exit
   ```

1. Log off node

   ```ShellSession
   $ logout
   ```

1. Copy messages file to the controller

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${NODE_PRIVATE_IP}:~/messages .
   ```

1. Log off controller

   ```ShellSession
   $ logout
   ```

1. Copy messages file to development machine

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}:~/messages ~/Downloads
   ```

1. Note that the messages file from the remote node can now be found here

   ```
   ~/Downloads/messages
   ```

## Appendix I: Connect to a running container

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

1. Set target environment

   *Example for "Dev" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   ```

1. Set controller access variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export CONTROLLER_PUBLIC_IP=52.7.241.208
   $ export SSH_USER_NAME=ec2-user
   ```

1. Copy the key to the controller

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}:~/.ssh
   ```
   
1. Connect to the controller

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
   ```

1. Set node variables

   *Example for "Dev" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   $ export NODE_PRIVATE_IP=10.242.26.231
   $ export SSH_USER_NAME=ec2-user
   ```

1. Connect to a node

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${NODE_PRIVATE_IP}
   ```

1. Connect to a running container

   *Example for connecting to an API container:*

   ```ShellSession
   $ docker exec -it $(docker ps -aqf "name=ecs-api-*" --filter "status=running") /bin/bash
   ```

   *Example for connecting to a worker container:*

   ```ShellSession
   $ docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") /bin/bash
   ```

## Appendix J: Delete and recreate database

1. Ensure that your are connected to CMS Cisco VPN before proceeding

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set AWS environment variables using the CloudTamer API

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Set target environment

   *Example for "Dev" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-east-impl
   ```

1. Set controller access variables
   
   ```ShellSession
   $ CONTROLLER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text)
   $ export SSH_USER_NAME=ec2-user
   ```
   
1. Connect to the controller
   
   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PRIVATE_IP}
   ```

1. Set target DB environment variables

   *Format:*
   
   ```ShellSession
   $ export MAIN_DB_NAME=postgres
   $ export TARGET_DB_NAME={target database name}
   $ export DB_HOST={database host}
   $ export DB_USER={database user}
   ```

1. Terminate all connections

   ```ShellSession
   $ psql --host  "${DB_HOST}" \
     --username "${DB_USER}" \
     --dbname "${MAIN_DB_NAME}" \
     --command "SELECT pg_terminate_backend(pg_stat_activity.pid) \
       FROM pg_stat_activity \
       WHERE pg_stat_activity.datname = '${TARGET_DB_NAME}' \
       AND pid <> pg_backend_pid();"
   ```

1. Delete the database

   ```ShellSession
   $ dropdb ${TARGET_DB_NAME} --host ${DB_HOST} --username ${DB_USER}
   ```

1. Create database

   ```ShellSession
   $ createdb ${TARGET_DB_NAME} --host ${DB_HOST} --username ${DB_USER}
   ```

## Appendix K: Complete DevOps linting checks

### Complete terraform linting

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Do a linting check

   ```ShellSession
   $ ./bash/tflint-check.sh
   ```

### Complete python linting

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Do a linting check

   ```ShellSession
   $ flake8 ./python3 > ~/Downloads/python3-linting.txt
   ```

1. Open the "python3-linting.txt" file and resolve any issues listed

   ```ShellSession
   $ vim ~/Downloads/python3-linting.txt
   ```

## Appendix L: View existing EUA job codes

1. Open Chrome

1. Enter the following in the address bar

   > https://eua.cms.gov/iam/im/pri/
   
1. Log on to EUA

   *Note that you should use the same username and password as you use for Jira and Confluence.*

1. Expand **EUA Info** in the leftmost panel

1. Select **Job Code Listing**

1. Select the following in the **Search for a group** section

   *Example searching for job codes with "SPLUNK" in the name:*
   
   ```
   where  Job Code  = *SPLUNK*	
   ```

## Appendix M: Make AB2D static website unavailable

### Delete the contents of the websites S3 bucket

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Delete all files from the "cms-ab2d-website" S3 bucket

   ```ShellSession
   $ aws --region us-east-1 s3 rm s3://cms-ab2d-website \
     --recursive
   ```

### Delete the cached website files from the CloudFront edge caches before they expire

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Invalidate the cloud distribution

   *Format:*
   
   ```ShellSession
   $ aws --region us-east-1 cloudfront create-invalidation \
     --distribution-id {cloudfront distribution id} \
     --paths "/*"
   ```

   *Example:*
   
   ```ShellSession
   $ aws --region us-east-1 cloudfront create-invalidation \
     --distribution-id E8P2KHG7IH0TG \
     --paths "/*"
   ```

1. Note the output

   *Format:*
   
   ```
   {
       "Location": "https://cloudfront.amazonaws.com/2019-03-26/distribution/{distribution id}/invalidation/{invalidation id}",
       "Invalidation": {
           "Id": "IKBNBOOJNM5OL",
           "Status": "InProgress",
           "CreateTime": "2019-12-16T15:22:16.488Z",
           "InvalidationBatch": {
               "Paths": {
                   "Quantity": 1,
                   "Items": [
                       "/*"
                   ]
               },
               "CallerReference": "cli-{caller reference id}"
           }
       }
   }
   ```

1. Note that the status says "InProgress"

1. Wait a few minutes to all the invalidation process to complete

1. Verify that the invalidation has completed

   1. Enter the following

      *Format:*
      
      ```ShellSession
      $ aws --region us-east-1 cloudfront list-invalidations \
        --distribution-id {cloudfront distribution id}
      ```

      *Example:*
      
      ```ShellSession
      $ aws --region us-east-1 cloudfront list-invalidations \
        --distribution-id E8P2KHG7IH0TG
      ```

   1. Verify that "Status" is "Completed"

      *Example:*
      
      ```
      {
          "InvalidationList": {
              "Items": [
                  {
                      "Id": "IKBNBOOJNM5OL",
                      "CreateTime": "2019-12-16T15:22:16.488Z",
                      "Status": "Completed"
                  }
              ]
          }
      }
      ```

1. Note that the invalidation will remain under the "Invalidations" tab for auditing purposes even if you redeploy the website

## Appendix N: Destroy and redploy API and Worker nodes

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Destroy and redploy API and Worker nodes

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ ./bash/redeploy-api-and-worker-nodes.sh \
     --profile=ab2d-dev \
     --environment=ab2d-dev \
     --vpc-id=vpc-0c6413ec40c5fdac3 \
     --ssh-username=ec2-user \
     --owner=842420567215 \
     --ec2-instance-type=m5.xlarge \
     --debug-level=WARN
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ ./bash/redeploy-api-and-worker-nodes.sh \
     --profile=ab2d-sbx-sandbox \
     --environment=ab2d-sbx-sandbox \
     --vpc-id=vpc-08dbf3fa96684151c \
     --ssh-username=ec2-user \
     --owner=842420567215 \
     --ec2-instance-type=m5.xlarge \
     --debug-level=WARN
   ```

## Appendix O: Destroy complete environment

1. Change to the deploy directory

   *Format:*
   
   ```ShellSession
   $ cd {code directory}/ab2d/Deploy
   ```

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```
   
1. Destroy the "dev" environment

   *Example to destroy the Dev environment:*
   
   ```ShellSession
   $ ./bash/destroy-environment.sh \
     --environment=ab2d-dev \
     --shared-environment=ab2d-dev-shared
   ```

   *Example to destroy the Dev environment, but preserve the AMIs:*
   
   ```ShellSession
   $ ./bash/destroy-environment.sh \
     --environment=ab2d-dev \
     --shared-environment=ab2d-dev-shared \
     --keep-ami
   ```

1. Destroy the "sbx" environment

   *Example to destroy the Sbx environment:*

   ```ShellSession
   $ ./bash/destroy-environment.sh \
     --environment=ab2d-sbx-sandbox \
     --shared-environment=ab2d-sbx-sandbox-shared
   ```

   *Example to destroy the Sbx environment, but preserve the AMIs:*
   
   ```ShellSession
   $ ./bash/destroy-environment.sh \
     --environment=ab2d-sbx-sandbox \
     --shared-environment=ab2d-sbx-sandbox-shared \
     --keep-ami
   ```

## Appendix P: Display disk space

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

1. Set target environment

   *Example for "Dev" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   ```

1. Set controller access variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export CONTROLLER_PUBLIC_IP=52.7.241.208
   $ export SSH_USER_NAME=ec2-user
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ export CONTROLLER_PUBLIC_IP=3.93.125.65
   $ export SSH_USER_NAME=ec2-user
   ```

1. Copy the key to the controller

   ```ShellSession
   $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}:~/.ssh
   ```
   
1. Connect to the controller
   
   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
   ```

1. Set node variables

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export TARGET_ENVIRONMENT=ab2d-dev
   $ export NODE_PRIVATE_IP=10.242.31.196
   $ export SSH_USER_NAME=ec2-user
   ```
   
1. Connect to node

   ```ShellSession
   $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${NODE_PRIVATE_IP}
   ```

1. Display the amount of disk space available on each file system in a human readable manner

   1. Enter the following

      ```ShellSession
      $ sudo df -h
      ```

   1. Note the output

      ```
      Filesystem                        Size  Used Avail Use% Mounted on
      devtmpfs                          7.6G     0  7.6G   0% /dev
      tmpfs                             7.6G   16K  7.6G   1% /dev/shm
      tmpfs                             7.6G   57M  7.5G   1% /run
      tmpfs                             7.6G     0  7.6G   0% /sys/fs/cgroup
      /dev/mapper/VolGroup00-rootVol     10G  2.4G  7.7G  24% /
      /dev/nvme0n1p1                   1014M  275M  740M  28% /boot
      /dev/mapper/VolGroup00-homeVol    3.0G   33M  3.0G   2% /home
      /dev/mapper/VolGroup00-tmpVol    1014M   34M  981M   4% /tmp
      /dev/mapper/VolGroup00-varVol     6.0G  1.8G  4.3G  30% /var
      /dev/mapper/VolGroup00-vartmpVol 1014M   33M  982M   4% /var/tmp
      /dev/mapper/VolGroup00-logVol     4.0G   43M  4.0G   2% /var/log
      /dev/mapper/VolGroup00-auditVol   4.0G   53M  4.0G   2% /var/log/audit
      overlay                           6.0G  1.8G  4.3G  30% /var/lib/docker/overlay2/806c002b8d5a7e51925854155c7f40808d6501908f17e58560a78e8e70a7d521/merged
      shm                                64M     0   64M   0% /var/lib/docker/containers/831e3b41737ee1174af0a93aa991543831565f3a523f62c35e026661d818bf7d/mounts/shm
      overlay                           6.0G  1.8G  4.3G  30% /var/lib/docker/overlay2/a608f39b66941b6333aaf7b6b1e0346f42f5f1c160a26528a4382da2b4fcb774/merged
      shm                                64M     0   64M   0% /var/lib/docker/containers/617e4a4ec4195e1f47d8e6e4c4e743eeb2d60c26309e42598d318fc53da9dd08/mounts/shm
      tmpfs                             1.6G     0  1.6G   0% /run/user/1000
      ```

## Appendix Q: Test API using swagger

1. Open Chrome

1. Enter the following in the address bar

   > https://confluence.cms.gov/display/AB2D/Step+By+Step+Tutorial

1. Note the following information from this document

   - Client Id

   - Client Password

   - username

   - password

   - OKTA server URL
   
1. Open Postman

1. Create an "ab2d" collection

   1. Select "New"

   1. Select "Collection"

   1. Enter the following on the "CREATE A NEW COLLECTION" page

      - **Name:** ab2d

   1. Select "Create"

1. Add an "retrieve-a-token" request to the "ab2d" collection

   1. Select **...** beside the "ab2d" collection

   1. Select **Add Request**

   1. Enter the following on the "SAVE REQUEST" page

      - **Request name:** retreive-a-token

   1. Select **Save to ab2d**

1. Configure the "retreive-a-token" request

   1. Expand the "ab2d" collection node

   1. Select the following

      ```
      GET retreive-a-token
      ```

   1. Change "GET" to "POST"

   1. Enter the noted OKTA server URL in the "Enter request URL" text box

   1. Select the **Params** tab

   1. Add the following key value pairs

      Key       |Value
      ----------|----------
      grant_type|password
      scope     |openid
      username  |{noted username}
      password  |{noted password}

   1. Note that add parameters now appear as part of the OKTA server URL

   1. Select the **Headers** tab

   1. Add the following key value pairs

      Key         |Value
      ------------|----------
      Content-Type|application/x-www-form-urlencoded
      Accept      |application/json
   
   1. Select the **Authorization** tab

   1. Select "Basic Auth" from the "TYPE" dropdown

   1. Configure the "Authorization" page

      - **Username:** {noted username}

      - **Password:** {noted password}

   1. Select **Send**

   1. Verify that you get an "access_token" within the JSON response

   1. Note the "access_token"

   1. Note that this "access_token" is the JWT access token that you will use within the swagger-ui)

   1. Note that you can get a cURL version of the request by doing the following

      1. Select **Code** to the far right of the tabbed options (Params, Authorization, etc.)

      1. Select **cURL** from the leftmost panel of the "GENERATE CODE SNIPPETS" page

      1. Note the "cURL" statement

1. Open Chrome

1. Enter the "swagger-ui/index.html" URL for the target environment in the address bar

1. Note the list of API endpoints that are displayed

1. Authorize the endpoints using the JWT access token

   1. Select **Authorize**

   1. Paste the JWT access token value in the **Value** text box using the following format

      *Format:*

      ```
      Bearer {jwt access token}
      ```

   1. Select **Authorize**

   1. Verify that "Authorized" is displayed under "Authorization (apiKey)"

   1. Select **Close**

   1. Verify that all the lock icons are now displayed as locked

   1. Note that locked icons does not mean that you typed in the authorization token correctly

1. Test the "/api/v1/fhir/Patient/$export" API

   1. Select **Export**

   1. Select **GET** beside the "/api/v1/fhir/Patient/$export" API

   1. Note the information about "Parameters" and "Responses"

   1. Select **Try it out**

   1. Configure the "Parameters" as follows

      - **Prefer:** respond-async
      
      - **_outputFormat:** application/fhir_ndjson
      
      - **_type:** ExplanationOfBenefits

   1. Select **Execute**

   1. Note the 202 response which means the export request has started

   1. Note the response

      *Example:*
      
      ```
      cache-control: no-cache, no-store, max-age=0, must-revalidate 
      connection: keep-alive 
      content-length: 0 
      content-location: http://{alb domain}/api/v1/fhir/Job/{job id}/$status 
      date: Tue, 14 Jan 2020 21:09:35 GMT 
      expires: 0 
      pragma: no-cache 
      x-content-type-options: nosniff 
      x-frame-options: DENY 
      x-xss-protection: 1; mode=block 
      ```

   1. Note the job id in the response

   1. Select **GET** again beside the "/api/v1/fhir/Patient/$export" API to collapse the section

1. Test the "/api/v1/fhir/Job/{jobUuid}/$status" API

   1. Select **Status**

   1. Select **GET** beside the "/api/v1/fhir/Job/{jobUuid}/$status" API

   1. Select **Try it out**

   1. Configure the "Parameters" as follows

      - **jobUuid:** {noted job id from the patient export api response}

   1. Select **Execute**
   
   1. Note the 200 response which means the job has completed

   1. Note the following attribute in the output

      *Format:*
      
      ```
      "url": "http://{alb domain}/api/v1/fhir/Job/{jobUuid}/file/{ndjson file}"
      ```

   1. Note the name of the ndjson file in the output

   1. Select **GET** again beside the "/api/v1/fhir/Job/{jobUuid}/$status" API to collapse the section

1. Test the "/api/v1/fhir/Job/{jobUuid}/file/{filename}" API

   1. Select **Download**

   1. Select **GET** beside the "/api/v1/fhir/Job/{jobUuid}/file/{filename}" API

   1. Select **Try it out**

   1. Configure the "Parameters" as follows

      - **Accept:** application/fhir+json

      - **filename:** {noted ndjson file}

      - **jobUuid:** {noted job id from the patient export api response}

   1. Select **Execute**

      > *** TO DO ***: determine why it isn't working

1. Verify EFS is working
   
   1. Set target AWS profile
   
      *Example for "Dev" environment:*
   
      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      ```
   
      *Example for "Sbx" environment:*
   
      ```ShellSession
      $ export AWS_PROFILE=ab2d-sbx-sandbox
      ```
   
   1. Set target environment
   
      *Example for "Dev" environment:*
   
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-dev
      ```
   
      *Example for "Sbx" environment:*
   
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      ```
   
   1. Set controller access variables
   
      *Example for "Dev" environment:*
      
      ```ShellSession
      $ export CONTROLLER_PUBLIC_IP=52.7.241.208
      $ export SSH_USER_NAME=ec2-user
      ```
   
      *Example for "Sbx" environment:*
      
      ```ShellSession
      $ export CONTROLLER_PUBLIC_IP=3.93.125.65
      $ export SSH_USER_NAME=ec2-user
      ```
   
   1. Copy the key to the controller
   
      ```ShellSession
      $ scp -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}:~/.ssh
      ```
      
   1. Connect to the controller
   
      *Format:*
      
      ```ShellSession
      $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${CONTROLLER_PUBLIC_IP}
      ```

1. Connect to each host node and docker container

   1. Note that you will want to repeat this section for api nodes 1-2 and worker nodes 1-2

   1. Set node variables
   
      *Example for "Dev" environment (api node 1):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-dev
      $ export NODE_PRIVATE_IP=10.242.26.201
      $ export SSH_USER_NAME=ec2-user
      ```

      *Example for "Dev" environment (api node 2):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-dev
      $ export NODE_PRIVATE_IP=10.242.26.34
      $ export SSH_USER_NAME=ec2-user
      ```

      *Example for "Dev" environment (worker node 1):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-dev
      $ export NODE_PRIVATE_IP=10.242.26.4
      $ export SSH_USER_NAME=ec2-user
      ```

      *Example for "Dev" environment (worker node 2):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-dev
      $ export NODE_PRIVATE_IP=10.242.26.196
      $ export SSH_USER_NAME=ec2-user
      ```

      *Example for "Sbx" environment (api node 1):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      $ export NODE_PRIVATE_IP=10.242.31.151
      $ export SSH_USER_NAME=ec2-user
      ```
   
      *Example for "Sbx" environment (api node 2):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      $ export NODE_PRIVATE_IP=10.242.31.120
      $ export SSH_USER_NAME=ec2-user
      ```
   
      *Example for "Sbx" environment (worker node 1):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      $ export NODE_PRIVATE_IP=10.242.31.153
      $ export SSH_USER_NAME=ec2-user
      ```
   
      *Example for "Sbx" environment (worker node 2):*
      
      ```ShellSession
      $ export TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      $ export NODE_PRIVATE_IP=10.242.31.25
      $ export SSH_USER_NAME=ec2-user
      ```
   
   1. Connect to a node
   
      *Format:*
   
      ```ShellSession
      $ ssh -i ~/.ssh/${TARGET_ENVIRONMENT}.pem ${SSH_USER_NAME}@${NODE_PRIVATE_IP}
      ```

   1. Verify the ndjson file for the job under the mounted EFS on the EC2 host

      *Format:*
      
      ```ShellSession
      $ cat /mnt/efs/{job id}/{ndjson file}
      ```

   1. Connect to a running container
   
      *Example for connecting to an API container:*
   
      ```ShellSession
      $ docker exec -it $(docker ps -aqf "name=ecs-api-*" --filter "status=running") /bin/bash
      ```
   
      *Example for connecting to a worker container:*
   
      ```ShellSession
      $ docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") /bin/bash
      ```

   1. Verify the ndjson file for the job under the mounted EFS in the docker container

      *Format:*
      
      ```ShellSession
      $ cat /mnt/efs/{job id}/{ndjson file}
      ```

   1. Exit the docker container

      ```ShellSession
      $ exit
      ```

   1. Exit the EC2 host node

      ```ShellSession
      $ exit
      ```

   1. Make sure that you repeat this section for all host nodes and docker containers

## Appendix R: Update userdata for auto scaling groups through the AWS console

1. Log on to AWS

1. Select **EC2**

1. Select **Launch Configurations** under "AUTO SCALING" in the leftmost panel

1. Modify the API launch configuration

   1. Select the API launch configuration

      *Format:*

      ```
      {environment}-api-{timestamp}
      ```

   1. Select **Copy launch configuarion**

   1. Select **Configure details**

   1. Expand **Advanced Details**

   1. Modify **User data** edit box as desired

   1. Select **Skip to review**

   1. Select **Create launch configuration**

   1. Note that the following is already filled out

      *Format:*
   
      - **First dropdown:** Choose an existing key pair

      - **Select a key pair:** {environment key pair}

   1. Check **I acknowledge...**

   1. Select **Create launch configuration**

   1. Select **Close**

1. Modify the worker launch configuration

   1. Select the worker launch configuration

      *Format:*

      ```
      {environment}-worker-{timestamp}
      ```

   1. Select **Copy launch configuarion**

   1. Select **Configure details**

   1. Expand **Advanced Details**

   1. Modify **User data** edit box as desired

   1. Select **Skip to review**

   1. Select **Create launch configuration**

   1. Note that the following is already filled out

      *Format:*
   
      - **First dropdown:** Choose an existing key pair

      - **Select a key pair:** {environment key pair}

   1. Check **I acknowledge...**

   1. Select **Create launch configuration**

   1. Select **Close**

1. Select **Auto Scaling Groups** under "AUTO SCALING" in the leftmost panel

1. Modify the API auto scaling group

   1. Select the API auto scaling group

      *Format:*

      ```
      {environment}-api-{timestamp}
      ```

   1. Select **Actions**

   1. Select **Edit**

   1. Select the newly created API launch configuration with "Copy" at the end of its name from the **Launch Configuration** dropdown

      *Format:*

      ```
      {environment}-api-{timestamp}Copy
      ```

   1. Select **Save**

1. Modify the worker auto scaling group

   1. Select the worker auto scaling group

      *Format:*

      ```
      {environment}-worker-{timestamp}
      ```

   1. Select **Actions**

   1. Select **Edit**

   1. Select the newly created worker launch configuration with "Copy" at the end of its name from the **Launch Configuration** dropdown

      *Format:*

      ```
      {environment}-worker-{timestamp}Copy
      ```

   1. Select **Save**

1. Terminate the instances that were created by the old launch configuration

1. Wait for the new launch configurations to create new API and worker instances

1. Verify that the API instances received the userdata change

   1. Select **Instances** in the leftmost panel

   1. Select one of the newly created API instances

   1. Select **Actions**

   1. Select **Instance Settings**

   1. Select **View/Change User Data**

   1. Verify the userdata changes that you made are in the **User data** edit box

   1. Select **Cancel**

1. Verify that the worker instances received the userdata change

   1. Select **Instances** in the leftmost panel

   1. Select one of the newly created worker instances

   1. Select **Actions**

   1. Select **Instance Settings**

   1. Select **View/Change User Data**

   1. Verify the userdata changes that you made are in the **User data** edit box

   1. Select **Cancel**

1. Delete the original API launch configuration

   1. Select **Lauch Configurations** under the "AUTO SCALING" section in the leftmost panel

   1. Select the original API launch configuration

      *Format:*

      ```
      {environment}-api-{timestamp}
      ```

   1. Select **Actions**

   1. Select **Delete launch configuration**

   1. Select **Yes, Delete**

1. Delete the original worker launch configuration

   1. Select **Lauch Configurations** under the "AUTO SCALING" section in the leftmost panel

   1. Select the original API launch configuration

      *Format:*

      ```
      {environment}-worker-{timestamp}
      ```

   1. Select **Actions**

   1. Select **Delete launch configuration**

   1. Select **Yes, Delete**

1. Update "tfstate" file

   1. Select **S3**

   1. Navigate to automation bucket path of the changed environment

      *Example for "Dev" environment:*

      ```
      ab2d-dev-automation/ab2d-dev/terraform
      ```
      
      *Example for "Sbx" environment:*

      ```
      ab2d-sbx-sandbox-automation/ab2d-sbx-sandbox/terraform
      ```

   1. Download the "tfstate" file

   1. Note the downloaded file's name changed to the following

      ```
      terraform.json
      ```

   1. Rename the "tfstate" file in S3 to the following

      ```
      terraform.tfstate.backup
      ```

   1. Open the "terraform.json" file

      ```ShellSession
      $ vim ~/Downloads/terraform.json
      ```

   1. Change the API "launch_configuration" line as follows

      ```
      "launch_configuration": "{environment}-api-{timestamp}Copy",     
      ```

   1. Change the worker "launch_configuration" line as follows

      ```
      "launch_configuration": "{environment}-worker-{timestamp}Copy",     
      ```

   1. Save and close the file

   1. Select **S3**

   1. Navigate to automation bucket path of the changed environment

      *Example for "Dev" environment:*

      ```
      ab2d-dev-automation/ab2d-dev/terraform
      ```
      
      *Example for "Sbx" environment:*

      ```
      ab2d-sbx-sandbox-automation/ab2d-sbx-sandbox/terraform
      ```

   1. Upload the modified "terraform.json" to S3

   1. Rename "terraform.json" in S3 to the following

      ```
      terraform.tfstate
      ```

## Appendix S: Install Ruby on RedHat linux

1. Install rbenv dependencies

   ```ShellSession
   $ sudo yum install -y git-core zlib zlib-devel gcc-c++ patch readline \
     readline-devel libyaml-devel libffi-devel openssl-devel make bzip2 \
     autoconf automake libtool bison curl sqlite-devel
   ```

1. Install rbenv and ruby-build

   ```ShellSession
   $ curl -sL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-installer | bash -
   ```

1. Note that will see output that looks similar to this

   ```
   Running doctor script to verify installation...
   Checking for `rbenv' in PATH: which: no rbenv in (/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/opt/puppetlabs/bin:/home/ec2-user/.local/bin:/home/ec2-user/bin)
   not found
     You seem to have rbenv installed in `/home/ec2-user/.rbenv/bin', but that
     directory is not present in PATH. Please add it to PATH by configuring
     your `~/.bashrc', `~/.zshrc', or `~/.config/fish/config.fish'.
   ```

1. Add rbenv to path

   ```ShellSession
   $ echo 'export PATH="$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
   $ echo 'eval "$(rbenv init -)"' >> ~/.bashrc
   $ source ~/.bashrc
   ```

1. Note that you can the determine that latest stable version of Ruby available via rbenv by doing the following

   ```ShellSession
   $ rbenv install -l | grep -v - | tail -1
   ```

1. Note the current pinned version of Ruby on your development machine

   1. Open a new terminal
   
   1. Enter the following on your Mac
   
      ```ShellSession
      $ ruby --version
      ```

   1. Note the base ruby version

      ```
      2.6.5
      ```

   1. Note that you will to use the same version on RedHat because that is the version that was used to test ruby scripts

1. Return to the RedHat node terminal session tab

1. Install the noted version of Ruby

   ```ShellSession
   $ rbenv install 2.6.5
   ```

1. Wait for the installation to complete

   *Note that the installation will take a while. Be patient.*
   
1. Set the global version of Ruby 
   
   ```ShellSession
   $ rbenv global 2.6.5
   ```

1. Verify the Ruby version

   ```ShellSession
   $ ruby --version
   ```

1. Install bundler

   ```ShellSession
   $ gem install bundler
   ```

1. Update Ruby Gems

   ```ShellSession
   $ gem update --system
   ```
   
## Appendix T: Test getting and decrypting a file from S3

1. Copy Gemfile and Rakefile to the controller

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ scp -i ~/.ssh/ab2d-dev.pem ruby/Gemfile ec2-user@52.7.241.208:/tmp
   $ scp -i ~/.ssh/ab2d-dev.pem ruby/Rakefile ec2-user@52.7.241.208:/tmp
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ scp -i ~/.ssh/ab2d-sbx-sandbox.pem ruby/Gemfile ec2-user@3.93.125.65:/tmp
   $ scp -i ~/.ssh/ab2d-sbx-sandbox.pem ruby/Rakefile ec2-user@3.93.125.65:/tmp
   ```

1. Connect to the controller

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@52.7.241.208
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbx-sandbox.pem ec2-user@3.93.125.65
   ```

1. Copy Gemfile and Rakefile to a worker node

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ scp -i ~/.ssh/ab2d-dev.pem /tmp/Gemfile ec2-user@10.242.26.249:/tmp
   $ scp -i ~/.ssh/ab2d-dev.pem /tmp/Rakefile ec2-user@10.242.26.249:/tmp
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ scp -i ~/.ssh/ab2d-sbx-sandbox.pem /tmp/Gemfile ec2-user@10.242.31.50:/tmp
   $ scp -i ~/.ssh/ab2d-sbx-sandbox.pem /tmp/Rakefile ec2-user@10.242.31.50:/tmp
   ```

1. Connect to the worker node where the Gemfile and Rakefile were copied

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@10.242.26.249
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbx-sandbox.pem ec2-user@10.242.31.50
   ```

1. Install rbenv dependencies

   ```ShellSession
   $ sudo yum install -y git-core zlib zlib-devel gcc-c++ patch readline \
     readline-devel libyaml-devel libffi-devel openssl-devel make bzip2 \
     autoconf automake libtool bison curl sqlite-devel
   ```

1. Install rbenv and ruby-build

   ```ShellSession
   $ curl -sL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-installer | bash -
   ```

1. Note that will see output that looks similar to this

   ```
   Running doctor script to verify installation...
   Checking for `rbenv' in PATH: which: no rbenv in (/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/opt/puppetlabs/bin:/home/ec2-user/.local/bin:/home/ec2-user/bin)
   not found
     You seem to have rbenv installed in `/home/ec2-user/.rbenv/bin', but that
     directory is not present in PATH. Please add it to PATH by configuring
     your `~/.bashrc', `~/.zshrc', or `~/.config/fish/config.fish'.
   ```

1. Add rbenv to path

   ```ShellSession
   $ echo 'export PATH="$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
   $ echo 'eval "$(rbenv init -)"' >> ~/.bashrc
   $ source ~/.bashrc
   ```

1. Note that you can the determine that latest stable version of Ruby available via rbenv by doing the following

   ```ShellSession
   $ rbenv install -l | grep -v - | tail -1
   ```

1. Note the current pinned version of Ruby on your development machine

   1. Open a new terminal
   
   1. Enter the following on your Mac
   
      ```ShellSession
      $ ruby --version
      ```

   1. Note the base ruby version

      ```
      2.6.5
      ```

   1. Note that you will to use the same version on RedHat because that is the version that was used to test ruby scripts

1. Return to the RedHat node terminal session tab

1. Install the noted version of Ruby

   ```ShellSession
   $ rbenv install 2.6.5
   ```

1. Wait for the installation to complete

   *Note that the installation will take a while. Be patient.*
   
1. Set the global version of Ruby 
   
   ```ShellSession
   $ rbenv global 2.6.5
   ```

1. Verify the Ruby version

   ```ShellSession
   $ ruby --version
   ```

1. Install bundler

   ```ShellSession
   $ gem install bundler
   ```

1. Update Ruby Gems

   ```ShellSession
   $ gem update --system
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Ensure required gems are installed

   ```ShellSession
   $ bundle install
   ```
   
1. Get keystore from S3 and decrypt it

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ bundle exec rake get_file_from_s3_and_decrypt['./test-file.txt','ab2d-dev-automation']
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ bundle exec rake get_file_from_s3_and_decrypt['./test-file.txt','ab2d-sbx-sandbox-automation']
   ```

   *Example for "Impl" environment:*

   > *** TO DO ***: Run this after deploying to IMPL

   ```ShellSession
   $ bundle exec rake get_file_from_s3_and_decrypt['./test-file.txt','ab2d-east-impl-automation']
   ```

1. Verify that decrypted file is in the "/tmp" directory

   ```ShellSession
   $ cat /tmp/test-file.txt
   ```

1. Create a "bfd-keystore" directory under EFS (if doesn't exist)

   ```ShellSession
   $ sudo mkdir -p /mnt/efs/bfd-keystore
   ```

1. Move file to the "bfd-keystore" directory

   ```ShellSession
   $ sudo mv /tmp/test-file.txt /mnt/efs/bfd-keystore
   ```

## Appendix U: Interact with the New Relic infrastructure agent

1. Connect to any EC2 instance

1. Check the status of the New Relic infrastructure agent

   ```ShellSession
   $ sudo systemctl status newrelic-infra
   ```

1. If New Relic infrastructure agent is stopped, you can start it by running the following command

   ```ShellSession
   $ sudo systemctl start newrelic-infra
   ```

1. If New Relic infrastructure agent is started, you can stop it by running the following command

   ```ShellSession
   $ sudo systemctl stop newrelic-infra
   ```

1. If New Relic infrastructure agent is started, you can stop and restart it by running the following command

   ```ShellSession
   $ sudo systemctl restart newrelic-infra
   ```

## Appendix V: Add a new environment variable for ECS docker containers

1. In order to add a new environment variable for api ECS docker containers, the following files should be changed

   - ~/code/ab2d/Deploy/deploy-ab2d-to-cms.sh

     - add "Create or get '???' secret" section

     - add an error check for a failue to retireve the secret

     - add to "not auto-approved" module.api call

     - add to "auto-approved" module.api call

   - ~/code/ab2d/Deploy/documentation/AB2D_Deployment.md

     - add data entry for '???' secret
   
   - ~/code/ab2d/Deploy/terraform/modules/api/main.tf

     - modify aws_ecs_task_definition

   - ~/code/ab2d/Deploy/terraform/modules/api/variables.tf

     - add variable

   - ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl/main.tf

     - add variable to the api call

   - ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl/variables.tf

     - add variable

   - ~/code/ab2d/Deploy/terraform/environments/ab2d-dev/main.tf

     - add variable to the api call
   
   - ~/code/ab2d/Deploy/terraform/environments/ab2d-dev/variables.tf

     - add variable
   
   - ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox/main.tf

     - add variable to the api call

   - ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox/variables.tf

     - add variable

1. In order to add a new environment variable for worker ECS docker containers, the following files should be changed

   - ~/code/ab2d/Deploy/deploy-ab2d-to-cms.sh

     - add "Create or get '???' secret" section

     - add an error check for a failue to retireve the secret

     - add to "not auto-approved" module.worker call

     - add to "auto-approved" module.worker call

   - ~/code/ab2d/Deploy/documentation/AB2D_Deployment.md

     - add data entry for '???' secret
   
   - ~/code/ab2d/Deploy/terraform/modules/worker/main.tf

     - modify aws_ecs_task_definition

   - ~/code/ab2d/Deploy/terraform/modules/worker/variables.tf

     - add variable

   - ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl/main.tf

     - add variable to the worker call

   - ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl/variables.tf

     - add variable

   - ~/code/ab2d/Deploy/terraform/environments/ab2d-dev/main.tf

     - add variable to the worker call
   
   - ~/code/ab2d/Deploy/terraform/environments/ab2d-dev/variables.tf

     - add variable
   
   - ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox/main.tf

     - add variable to the worker call

   - ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox/variables.tf

     - add variable

## Appendix W: Launch a base EC2 instance that is created from gold disk AMI

1. Open Chrome

1. Log to AWS account

1. Select **EC2**

1. Select **Instances** in the leftmost panel

1. Select **Launch Instance**

1. Enter the following in the "Search for an AMI by entering a search term" text box

   ```
   842420567215
   ```

1. Note that a "results" link will appear in the main part of the page

   *Example:**

   ```
   12 results
   ```

1. Select the "XX results" link (where XX equals the number of results)

   *Example:**

   ```
   12 results
   ```

1. Uncheck **Owned by me** in the leftmost panel

1. Select the most recent "EAST-RH 7-7 Gold Image*" on the "Step 1: Choose an Amazon Machine Image (AMI)" page

   *Example:*

   ```
   EAST-RH 7-7 Gold Image V.1.05 (HVM) 01-23-20 - ami-0f2d8f925de453e46
   Root device type: ebs Virtualization type: hvm Owner: 842420567215 ENA Enabled: Yes
   ```

1. Select the following
    
   ```
   m5.xlarge
   ```

1. Select **Next: Configure Instance Details**

1. Configre the instance details as follows

   - **Number of instances:** 1

   - **Purchasing option:** {unchecked}

   - **Network:** {vpc id} | ab2d-mgmt-east-dev

   - **Subnet:** {subnet id} | ab2d-mgmt-east-dev-public-a | us-east-1a

   - **Auto-assign Public IP:** Enable

   - **Placement group:** {unchecked}

   - **Capacity reservation:** Open

   - **IAM Role:** Ab2dInstanceProfile

   - **CPU Options:** {unchecked}

   - **Shutdown behavior:** Stop

   - **Stop - Hibernate behavior:** {unchecked}

   - **Enable terminate protection:** {checked}

   - **Monitoring:** {unchecked}

   - **EBS-optimized instance:** {checked}

   - **Tenancy:** Shared - Run a shared hardware instance

   - **Elastic Inference:** {unchecked}

1. Select **Next: Add Storage**

1. Change "Size (GiB)" for the "Root" volume

   ```
   250
   ```

1. Select **Next: Add Tags**

1. Select **Next:: Configure Security Group**

1. Configure the "Step 6: Configure Security Group" page as follows

   - **Create a new security group:** {selected}

   - **Security group name:** ab2d-mgmt-east-dev-jenkins-sg

   - **Description:** ab2d-mgmt-east-dev-jenkins-sg

   - **Type:** All traffic

   - **Protocol:** All

   - **Port range:** 0 - 65535

   - **Source:** 10.232.32.0/19

   - **Description:** VPN Access

1. Select **Review and Launch**

1. Review the settings

1. Select **Launch**

1. Select the following from the first dropdown

   ```
   Choose an existing key pair
   ```

1. Select the following from the **Select a key pair** dropdown

   ```
   ab2d-mgmt-east-dev
   ```

1. Check **I acknowledge...**

1. Select **Launch Instances**

1. Select **aws** on the top left of the page

1. Select **EC2**

1. Select **Instances**

1. Wait for the instance to finish starting up with successful status checks

1. SSH into the instance

1. View the available disk devices

   1. Enter the following
   
      ```ShellSession
      $ lsblk
      ```

   1. Note the output

      *Example for an "m5.xlarge" instance:*

      ```
      nvme0n1                  259:0    0  30G  0 disk 
      ├─nvme0n1p1              259:1    0   1G  0 part /boot
      └─nvme0n1p2              259:2    0  29G  0 part 
        ├─VolGroup00-auditVol  253:0    0   4G  0 lvm  /var/log/audit
        ├─VolGroup00-homeVol   253:1    0   3G  0 lvm  /home
        ├─VolGroup00-logVol    253:2    0   4G  0 lvm  /var/log
        ├─VolGroup00-rootVol   253:3    0  10G  0 lvm  /
        ├─VolGroup00-tmpVol    253:4    0   2G  0 lvm  /tmp
        ├─VolGroup00-varVol    253:5    0   5G  0 lvm  /var
        └─VolGroup00-vartmpVol 253:6    0   1G  0 lvm  /var/tmp
      ```

   1. Note the root device in the output

      *Example for an "m5.xlarge" instance:*

      - nvme0n1

   1. Note the existing partitions

      *Example for an "m5.xlarge" instance:*

      - nvme0n1p1

      - nvme0n1p2

1. Create a new partition from unallocated space

   1. View the available disk devices again
   
      ```ShellSession
      $ lsblk
      ```

   1. Note the output

      *Example for an "m5.xlarge" instance:*

      ```
      nvme0n1                  259:0    0  30G  0 disk 
      ├─nvme0n1p1              259:1    0   1G  0 part /boot
      └─nvme0n1p2              259:2    0  29G  0 part 
        ├─VolGroup00-auditVol  253:0    0   4G  0 lvm  /var/log/audit
        ├─VolGroup00-homeVol   253:1    0   3G  0 lvm  /home
        ├─VolGroup00-logVol    253:2    0   4G  0 lvm  /var/log
        ├─VolGroup00-rootVol   253:3    0  10G  0 lvm  /
        ├─VolGroup00-tmpVol    253:4    0   2G  0 lvm  /tmp
        ├─VolGroup00-varVol    253:5    0   5G  0 lvm  /var
        └─VolGroup00-vartmpVol 253:6    0   1G  0 lvm  /var/tmp
      ```

   1. Note the root device in the output

      *Example for an "m5.xlarge" instance:*

      - nvme0n1

   1. Enter the following:
   
      *Format:*
   
      ```ShellSession
      $ sudo fdisk /dev/{root device}
      ```
   
      *Example for an "m5.xlarge" instance:*
   
      ```ShellSession
      $ sudo fdisk /dev/nvme0n1
      ```

   1. Request that the operating system re-reads the partition table

      ```ShellSession
      $ sudo partprobe
      ```

   1. View the available disk devices again
   
      ```ShellSession
      $ lsblk
      ```

   1. Note the output

      *Example for an "m5.xlarge" instance:*
      
      ```
      nvme0n1                  259:0    0  250G  0 disk 
      ├─nvme0n1p1              259:1    0    1G  0 part /boot
      ├─nvme0n1p2              259:2    0   29G  0 part 
      │ ├─VolGroup00-auditVol  253:0    0    4G  0 lvm  /var/log/audit
      │ ├─VolGroup00-homeVol   253:1    0    3G  0 lvm  /home
      │ ├─VolGroup00-logVol    253:2    0    4G  0 lvm  /var/log
      │ ├─VolGroup00-rootVol   253:3    0   10G  0 lvm  /
      │ ├─VolGroup00-tmpVol    253:4    0    2G  0 lvm  /tmp
      │ ├─VolGroup00-varVol    253:5    0    5G  0 lvm  /var
      │ └─VolGroup00-vartmpVol 253:6    0    1G  0 lvm  /var/tmp
      └─nvme0n1p3              259:3    0  220G  0 part 
      ```

   1. Note the newly creating partition

      *Example for an "m5.xlarge" instance:*

      - nvme0n1p3

1. Extend home partition

   1. View the available disk devices again
   
      ```ShellSession
      $ lsblk
      ```

   1. Note the output

      *Example for an "m5.xlarge" instance:*
      
      ```
      nvme0n1                  259:0    0  250G  0 disk 
      ├─nvme0n1p1              259:1    0    1G  0 part /boot
      ├─nvme0n1p2              259:2    0   29G  0 part 
      │ ├─VolGroup00-auditVol  253:0    0    4G  0 lvm  /var/log/audit
      │ ├─VolGroup00-homeVol   253:1    0    3G  0 lvm  /home
      │ ├─VolGroup00-logVol    253:2    0    4G  0 lvm  /var/log
      │ ├─VolGroup00-rootVol   253:3    0   10G  0 lvm  /
      │ ├─VolGroup00-tmpVol    253:4    0    2G  0 lvm  /tmp
      │ ├─VolGroup00-varVol    253:5    0    5G  0 lvm  /var
      │ └─VolGroup00-vartmpVol 253:6    0    1G  0 lvm  /var/tmp
      └─nvme0n1p3              259:3    0  220G  0 part 
      ```

   1. Note the newly creating partition again

      *Example for an "m5.xlarge" instance:*

      - nvme0n1p3

   1. Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

      ```ShellSession
      $ sudo pvcreate /dev/nvme0n1p3
      ```

   1. Note the output

      ```
      Physical volume "/dev/nvme0n1p3" successfully created.
      ```

   1. View the available disk devices again
   
      ```ShellSession
      $ lsblk
      ```

   1. Note the output

      *Example for an "m5.xlarge" instance:*
      
      ```
      nvme0n1                  259:0    0  250G  0 disk 
      ├─nvme0n1p1              259:1    0    1G  0 part /boot
      ├─nvme0n1p2              259:2    0   29G  0 part 
      │ ├─VolGroup00-auditVol  253:0    0    4G  0 lvm  /var/log/audit
      │ ├─VolGroup00-homeVol   253:1    0    3G  0 lvm  /home
      │ ├─VolGroup00-logVol    253:2    0    4G  0 lvm  /var/log
      │ ├─VolGroup00-rootVol   253:3    0   10G  0 lvm  /
      │ ├─VolGroup00-tmpVol    253:4    0    2G  0 lvm  /tmp
      │ ├─VolGroup00-varVol    253:5    0    5G  0 lvm  /var
      │ └─VolGroup00-vartmpVol 253:6    0    1G  0 lvm  /var/tmp
      └─nvme0n1p3              259:3    0  220G  0 part 
      ```

   1. Note the volume group

      ```
      VolGroup00
      ```

   1. Add the new physical volume to the volume group

      ```ShellSession
      $ sudo vgextend VolGroup00 /dev/nvme0n1p3
      ```

   1. Note the output

      ```
      Volume group "VolGroup00" successfully extended
      ```

   1. Extend the size of the home logical volume

      ```ShellSession
      $ sudo lvextend -l +100%FREE /dev/mapper/VolGroup00-homeVol
      ```

   1. Note the output

      ```
      Size of logical volume VolGroup00/homeVol changed from <3.00 GiB (767 extents) to 222.99 GiB (57086 extents).
  Logical volume VolGroup00/homeVol successfully resized.
      ```

   1. Expands the existing XFS filesystem

      ```ShellSession
      $ sudo xfs_growfs -d /dev/mapper/VolGroup00-homeVol
      ```

   1. Note the output

      ```
      meta-data=/dev/mapper/VolGroup00-homeVol isize=512    agcount=4, agsize=196352 blks
               =                       sectsz=512   attr=2, projid32bit=1
               =                       crc=1        finobt=0 spinodes=0
      data     =                       bsize=4096   blocks=785408, imaxpct=25
               =                       sunit=0      swidth=0 blks
      naming   =version 2              bsize=4096   ascii-ci=0 ftype=1
      log      =internal               bsize=4096   blocks=2560, version=2
               =                       sectsz=512   sunit=0 blks, lazy-count=1
      realtime =none                   extsz=4096   blocks=0, rtextents=0
      data blocks changed from 785408 to 58456064
      ```

## Appendix X: Verify access to the opt-out S3 bucket from sandbox worker nodes

### Test getting a public S3 file using AWS CLI and no sign request

1. Open a new terminal

1. Change to the abd2 repo code directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```
   
1. Gather opt-out S3 bucket information

   ```ShellSession
   $ cat optout/src/main/resources/application.optout.properties | grep "s3."
   ```

1. Note the output

   ```
   s3.region=${AB2D_S3_REGION:us-east-1}
   s3.bucket=${AB2D_S3_OPTOUT_BUCKET:ab2d-optout-data-dev}
   s3.filename=${AB2D_S3_OPTOUT_FILE:T#EFT.ON.ACO.NGD1800.DPRF.D191029.T1135430}
   ```

1. Delete s3 file (if exists locally)

   ```ShellSession
   $ rm -f /tmp/T#EFT.ON.ACO.NGD1800.DPRF.D191029.T1135430
   ```

1. Note the following

   - the S3 file that we want to download is from a public s3 bucket

   - in order to download a public S3 file without requiring AWS credentials, we need to include the "--no-sign-request" parameter

1. Test getting the file from local machine

   ```ShellSession
   $ aws --region us-east-1 --no-sign-request s3 cp \
     s3://ab2d-optout-data-dev/T#EFT.ON.ACO.NGD1800.DPRF.D191029.T1135430 \
     /tmp/.
   ```

### Test downloading a public S3 file using the AWS CLI without credentials

1. Open a new terminal

1. Test getting a public S3 file

   ```ShellSession
   $ aws --region us-east-1 s3 cp \
     s3://ab2d-optout-data-dev/T#EFT.ON.ACO.NGD1800.DPRF.D191029.T1135430 \
     /tmp/.
   ```

1. Note that this will fail with the following output

   ```
   fatal error: Unable to locate credentials
   ```

### Test downloading a public S3 file using the AWS CLI with the "ab2d-s3-signing" profile

1. Open a new terminal

1. Set AWS profile

   ```ShellSession
   $ export AWS_PROFILE=ab2d-s3-signing
   ```

1. Test getting a public S3 file

   ```ShellSession
   $ aws --region us-east-1 s3 cp \
     s3://ab2d-optout-data-dev/T#EFT.ON.ACO.NGD1800.DPRF.D191029.T1135430 \
     /tmp/.
   ```

### Test interacting with a public S3 file using the AWS Java SDK with environment variables

1. Change to the "s3-client-test" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/java/s3-client-test-workspace/s3-client-test
   ```

1. Build "s3-client-test"

   ```ShellSession
   $ mvn clean package
   ```

1. Set AWS region

   ```ShellSession
   $ export AWS_REGION={'ab2d-s3-signing AWS region' in 1Password}
   ```

1. Set AWS access key id

   ```ShellSession
   $ export AWS_ACCESS_KEY_ID={'ab2d-s3-signing AWS access key id' in 1Password}
   ```

1. Set AWS secret access key

   ```ShellSession
   $ export AWS_SECRET_ACCESS_KEY={'ab2d-s3-signing AWS secret access key' in 1Password}
   ```

1. Test interacting with a public S3 file

   ```ShellSession
   $ java -jar target/s3client-0.0.1-SNAPSHOT.jar
   ```

### Test interacting with a public S3 file on a worker node

1. Change to the "s3-client-test" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/java/s3-client-test-workspace/s3-client-test
   ```

1. Build "s3-client-test"

   ```ShellSession
   $ mvn clean package
   ```

1. Delete existing zipped target directory (if exists)

   ```ShellSession
   $ rm -f target.tgz
   ```

1. Zip up the target directory

   ```ShellSession
   $ tar -czvf target.tgz target
   ```

1. Copy the zipped target directory to a worker node

   *Format:*

   ```
   $ scp -i ~/.ssh/ab2d-dev.pem \
     -o ProxyCommand="ssh ec2-user@{controller private ip} nc {worker private ip} 22" \
	target.tgz \
	ec2-user@{worker private ip}:~
   ```

   *Example for Dev environment:*

   ```
   $ scp -i ~/.ssh/ab2d-dev.pem \
     -o ProxyCommand="ssh ec2-user@10.242.5.190 nc 10.242.26.94 22" \
	target.tgz \
	ec2-user@10.242.26.94:~
   ```

1. Connect to the worker node

   *Format:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@{worker private ip} \
     -o ProxyCommand="ssh -W %h:%p ec2-user@{controller private ip}"
   ```
   
   *Example for Dev environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@10.242.26.94 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.5.190"
   ```

   *Example for Sbx environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbx-sandbox.pem ec2-user@10.242.31.184 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.36.49"
   ```

   *Example for Impl environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-east-impl.pem ec2-user@10.242.133.14 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.132.76"
   ```

1. Copy zipped target directory to a worker docker container
   
   ```ShellSession
   $ docker cp target.tgz $(docker ps -aqf "name=ecs-worker-*" --filter "status=running"):/tmp
   ```

1. Connect to a running container

   *Example for connecting to a worker container:*

   ```ShellSession
   $ docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") /bin/bash
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Delete target directory (if exists)

   ```ShellSession
   $ rm -rf target
   ```

1. Unzip the zipped target file

   ```ShellSession
   $ tar -xzf target.tgz
   ```

1. Run the jar file

   ```ShellSession
   $ java -jar target/s3client-0.0.1-SNAPSHOT.jar
   ```

## Appendix Y: Test the opt-out process using IntelliJ

1. Open a terminal

1. Change to the "ab2d" repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```
   
1. Start up the database container

   ```ShellSession
   $ docker-compose up db
   ```

1. Open the ab2d project in IntelliJ

1. Open the following file in IntelliJ

   ```
   optout/src/main/resources/application.optout.properties
   ```

1. Note the cron schedule is currently set to run very hour at minute 0, second 0

   ```
   cron.schedule=0 0 * * * ?
   ```
   
1. Temporarily modify the cron schedule line to run every minute

   ```
   cron.schedule=0 * * * * ?
   ```

1. Add desired debug breakpoints to the following file

   ```
   optout/src/main/java/gov/cms/ab2d/optout/gateway/S3GatewayImpl.java
   ```

1. Select the "Worker" configuration from the dropdown in the upper right of the page

1. Select the debug icon toolbar button

1. Wait for the cron job to trigger so that it reaches the first breakpoint in the "S3GatewayImpl.java" file

1. After testing is complete, be sure to revert the "application.optout.properties" file

   ```ShellSession
   $ git checkout -- optout/src/main/resources/application.optout.properties
   ```

## Appendix Z: Test configuration of JVM settings within a container

### Check the default "heapsize" and "maxram" settings for an api node

1. Connect to a api node

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@{api private ip} \
     -o ProxyCommand="ssh -W %h:%p ec2-user@{controller private ip}"
   ```

   *Example for Dev environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@10.242.26.23 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.5.190"
   ```

   *Example for Sbx environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbx-sandbox.pem ec2-user@10.242.31.133 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.36.49"
   ```

1. Connect to the running api container

   ```ShellSession
   $ docker exec -it $(docker ps -aqf "name=ecs-api-*" --filter "status=running") /bin/bash
   ```

1. Get the default "heapsize" and "maxram" settings

   ```ShellSession
   $ java -XX:+PrintFlagsFinal -version | grep -Ei "maxheapsize|maxram"
   ```

1. Note the output

   *Example for Dev environment:*

   ```
   size_t MaxHeapSize           = 2147483648            {product} {ergonomic}
   uint64_t MaxRAM              = 137438953472       {pd product} {default}
   uintx MaxRAMFraction         = 4                     {product} {default}
   double MaxRAMPercentage      = 25.000000             {product} {default}
   size_t SoftMaxHeapSize       = 2147483648         {manageable} {ergonomic}
   ```

1. Exit the container

   ```ShellSession
   $ exit
   ```

1. Exit the api node

   ```ShellSession
   $ exit
   ```

### Verify JVM parameters for an api node

1. Connect to a api node

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@{api private ip} \
     -o ProxyCommand="ssh -W %h:%p ec2-user@{controller private ip}"
   ```

   *Example for Dev environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@10.242.26.23 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.5.190"
   ```

   *Example for Sbx environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbx-sandbox.pem ec2-user@10.242.31.133 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.36.49"
   ```

1. Connect to the running api container

   ```ShellSession
   $ docker exec -it $(docker ps -aqf "name=ecs-api-*" --filter "status=running") /bin/bash
   ```

1. Verify JVM parameters for API

   ```ShellSession
   $ jps -lvm | grep api
   ```

1. Note the output

   ```
   1 api-0.0.1-SNAPSHOT.jar -XX:+UseContainerSupport -XX:InitialRAMPercentage=40.0 -XX:MinRAMPercentage=20.0 -XX:MaxRAMPercentage=80.0 -javaagent:/usr/src/ab2d-api/newrelic/newrelic.jar
   ```

1. Exit the container

   ```ShellSession
   $ exit
   ```

1. Exit the api node

   ```ShellSession
   $ exit
   ```

### Check the default "heapsize" and "maxram" settings for a worker node

1. Connect to a worker node

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@{worker private ip} \
     -o ProxyCommand="ssh -W %h:%p ec2-user@{controller private ip}"
   ```

   *Example for Dev environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@10.242.26.47 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.5.190"
   ```

   *Example for Sbx environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbx-sandbox.pem ec2-user@10.242.31.110 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.36.49"
   ```

1. Connect to the running worker container

   ```ShellSession
   $ docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") /bin/bash
   ```

1. Get the default "heapsize" and "maxram" settings

   ```ShellSession
   $ java -XX:+PrintFlagsFinal -version | grep -Ei "maxheapsize|maxram"
   ```

1. Note the output

   *Example for Dev environment:*

   ```
   size_t MaxHeapSize           = 2147483648            {product} {ergonomic}
   uint64_t MaxRAM              = 137438953472       {pd product} {default}
   uintx MaxRAMFraction         = 4                     {product} {default}
   double MaxRAMPercentage      = 25.000000             {product} {default}
   size_t SoftMaxHeapSize       = 2147483648         {manageable} {ergonomic}
   ```

1. Exit the container

   ```ShellSession
   $ exit
   ```

1. Exit the worker node

   ```ShellSession
   $ exit
   ```

### Verify JVM parameters for a worker node

1. Connect to a worker node

   *Format:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@{worker private ip} \
     -o ProxyCommand="ssh -W %h:%p ec2-user@{controller private ip}"
   ```

   *Example for Dev environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-dev.pem ec2-user@10.242.26.47 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.5.190"
   ```

   *Example for Sbx environment:*

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbx-sandbox.pem ec2-user@10.242.31.110 \
     -o ProxyCommand="ssh -W %h:%p ec2-user@10.242.36.49"
   ```

1. Connect to the running worker container

   ```ShellSession
   $ docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") /bin/bash
   ```

1. Verify JVM parameters for API

   ```ShellSession
   $ jps -lvm | grep worker
   ```

1. Note the output

   ```
   1 worker-0.0.1-SNAPSHOT.jar -XX:+UseContainerSupport -XX:InitialRAMPercentage=40.0 -XX:MinRAMPercentage=20.0 -XX:MaxRAMPercentage=80.0 -javaagent:/usr/src/ab2d-worker/newrelic/newrelic.jar
   ```

1. Exit the container

   ```ShellSession
   $ exit
   ```

1. Exit the api node

   ```ShellSession
   $ exit
   ```

## Appendix AA: View CCS Cloud VPN Public IPs

1. Open Chrome

1. Enter the following in the address bar

   > https://confluence.cms.gov/pages/viewpage.action?spaceKey=AWSOC&title=CCS+Cloud+VPN+Public+IPs

## Appendix BB: Update controller

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Switch to development AWS profile

   *Example for Dev environment:*
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for Sbx environment:*
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

   *Example for Impl environment:*
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-east-impl
   ```

1. Set the target environment

   *Example for Dev environment:*

   ```ShellSession
   $ CMS_ENV=ab2d-dev
   ```

   *Example for Sbx environment:*

   ```ShellSession
   $ CMS_ENV=ab2d-sbx-sandbox
   ```

   *Example for Impl environment:*

   ```ShellSession
   $ CMS_ENV=ab2d-east-impl
   ```

1. Set the shared environment

   *Example for Dev environment:*
   
   ```ShellSession
   $ CMS_SHARED_ENV=ab2d-dev-shared
   ```

   *Example for Sbx environment:*
   
   ```ShellSession
   $ CMS_SHARED_ENV=ab2d-sbx-sandbox-shared
   ```

   *Example for Impl environment:*
   
   ```ShellSession
   $ CMS_SHARED_ENV=ab2d-east-impl-shared
   ```

1. Change to the python3 directory

   ```ShellSession
   $ cd python3
   ```

1. Set database secret datetime

   ```ShellSession
   $ DATABASE_SECRET_DATETIME="2020-01-02-09-15-01"
   ```
   
1. Get database user

   ```ShellSession
   $ DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
   ```

1. Get database password

   ```ShellSession
   $ DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
   ```

1. Get database name

   ```ShellSession
   $ DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
   ```

1. Get deployer IP address

   ```ShellSession
   $ DEPLOYER_IP_ADDRESS=$(curl ipinfo.io/ip)
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Change to the shared environment

   ```ShellSession
   $ cd "terraform/environments/${CMS_SHARED_ENV}"
   ```

1. Verify the changes that will be made

   ```ShellSession
   $ terraform plan \
     --var "db_username=${DATABASE_USER}" \
     --var "db_password=${DATABASE_PASSWORD}" \
     --var "db_name=${DATABASE_NAME}" \
     --var "deployer_ip_address=${DEPLOYER_IP_ADDRESS}" \
     --target module.controller
   ```

1. Update controller

   ```ShellSession
   $ terraform apply \
     --var "db_username=${DATABASE_USER}" \
     --var "db_password=${DATABASE_PASSWORD}" \
     --var "db_name=${DATABASE_NAME}" \
     --var "deployer_ip_address=${DEPLOYER_IP_ADDRESS}" \
     --target module.controller \
     --auto-approve
   ```

## Appendix CC: Fix bad terraform component

1. Note the component that was failing to refresh when automation was rerun

   *Example of a component that was failing to refresh:*
   
   ```
   module.controller.random_shuffle.public_subnets
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set AWS environment variables using the CloudTamer API

   ```ShellSession
   $ source ./bash/set-env.sh
   ```
   
1. Change to the environment where the existing component is failing to refresh

   *Example for Dev shared environment:*
   
   ```ShellSession
   $ cd terraform/environments/ab2d-dev-shared
   ```

1. Verify that the component is under terraform control

   *Format:*
   
   ```ShellSession
   $ terraform state list | grep {search word}
   ```

   *Example for 'module.controller.random_shuffle.public_subnets':*
   
   ```ShellSession
   $ terraform state list | grep shuffle
   ```

1. Note the terraform reference for the target component

   ```
   module.controller.random_shuffle.public_subnets
   ```

1. Remove the component from terraform control

   ```ShellSession
   $ terraform state rm module.controller.random_shuffle.public_subnets
   ```

1. Rerun the automation, so that the module will be recreated instead of being refreshed

## Appendix DD: Test running development automation from Jenkins master

1. Set the management AWS profile

   ```ShellSession
   $ export AWS_PROFILE=ab2d-mgmt-east-dev
   ```

1. Connect to the Jenkins EC2 instance

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_MASTER_PUBLIC_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PublicIpAddress" \
        --output text)
      ```

   1. Ensure that you are connected to the Cisco VPN

   1. SSH into the instance using the private IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PUBLIC_IP
      ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Create code directory

   ```ShellSession
   $ mkdir -p ~/code
   ```

1. Change to the "code" directory

   ```ShellSession
   $ cd ~/code
   ```

1. Clone the ab2d repo

   ```ShellSession
   $ git clone https://github.com/CMSgov/ab2d.git
   ```

1. Change to the repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Checkout the desired branch

   *Format:*

   ```ShellSession
   $ git checkout {desired branch}
   ```

   *Example:*

   ```ShellSession
   $ git checkout feature/ab2d-573-create-jenkins-scripts-for-automation-hooks
   ```

1. Verify that python3 is working

   1. Change to the "python3" directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy/python3
      ```

   1. Set the test environment variables

      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      $ export CMS_ENV=ab2d-dev
      $ export DATABASE_SECRET_DATETIME=2020-01-02-09-15-01
      ```

   1. Test getting the database user for the development environment

      ```ShellSession
      $ DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
      ```

   1. Verify that the database user was retrieved

      ```ShellSession
      $ echo $DATABASE_USER
      ```

   1. Exit the jenkins user so that environment variables can be reset

      ```ShellSession
      $ exit
      ```

1. Connect to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set test parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev
   $ export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev
   $ export REGION_PARAM=us-east-1
   $ export VPC_ID_PARAM=vpc-0c6413ec40c5fdac3
   $ export SSH_USERNAME_PARAM=ec2-user
   $ export EC2_INSTANCE_TYPE_API_PARAM=m5.xlarge
   $ export EC2_INSTANCE_TYPE_WORKER_PARAM=m5.xlarge
   $ export EC2_DESIRED_INSTANCE_COUNT_API_PARAM=1
   $ export EC2_MINIMUM_INSTANCE_COUNT_API_PARAM=1
   $ export EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM=1
   $ export EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM=1
   $ export EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM=1
   $ export EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM=1
   $ export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export INTERNET_FACING_PARAM=false
   $ export CLOUD_TAMER_PARAM=true
   ```

1. Run application deployment automation

   ```ShellSession
   $ ./bash/deploy-application.sh
   ```

## Appendix EE: Fix Jenkins reverse proxy error

1. Ensure that you are connected to the Cisco VPN

1. Open Chrome

1. Update Jenkins URL for GitHub OAuth application

   1. Lonnie logs onto his GitHub account

   1. Select the GitHub profile icon in the top left of the page

   1. Select **Settings**

   1. Select **Developer settings** from the leftmost panel

   1. Select **OAuth Apps** from the leftmost panel

   1. Select the following OAuth app

      ```
      jenkins-github-authentication
      ```

   1. Change the **Authorization callback URL** to use the desired IP address

      *Example of using the private IP address:**

      > http://{jenkins master private ip address}:8080/securityRealm/finishLogin

      *Example of using the public IP address:**

      > http://{jenkins master public ip address}:8080/securityRealm/finishLogin


   1. If you changed the "Authorization callback URL", select **Update application**

1. Enter the Jenkins master URL based on the IP address used to configure the "Authorization callback URL"

   *Format:*

   > http://{jenkins ip address used for github authentication}:8080

   *Example if GitHub integration is configured with private IP address:*

   > http://{jenkins master private ip}:8080

   *Example if GitHub integration is configured with public IP address:*

   > http://{jenkins master public ip}:8080

1. Select **Manage Jenkins**

1. Note that if you see the following error, these instructions will fix the issue

   ```
   It appears that your reverse proxy set up is broken.
   ```

1. Select **Configure System**

1. Type the following in the **Jenkins URL** text box under the "Jenkins Location" section

   *Format:*

   > http://{jenkins ip address used for github authentication}:8080

   *Example if GitHub integration is configured with private IP address:*

   > http://{jenkins master private ip}:8080

   *Example if GitHub integration is configured with public IP address:*

   > http://{jenkins master public ip}:8080

1. Select **Apply**

1. Select **Save**

1. Select **Manage Jenkins** again

1. Note that the reverse proxy warning no longer appears

## Appendix FF: Manually add JDK 13 to a node that already has JDK 8

1. Connect to desired node

1. Install JDK 13

   ```ShellSession
   $ sudo yum install java-13-openjdk-devel -y
   ```

1. Get JDK 13 java binary

   ```ShellSession
   $ JAVA_13=$(alternatives --display java | grep family | grep java-13-openjdk | cut -d' ' -f1)
   ```
   
1. Set java binary to JDK 13

   ```ShellSession
   $ sudo alternatives --set java $JAVA_13
   ```

1. Get JDK 13 javac binary

   ```ShellSession
   $ JAVAC_13=$(alternatives --display javac | grep family | grep java-13-openjdk | cut -d' ' -f1)
   ```
   
1. Set javac binary to JDK 13

   ```ShellSession
   $ sudo alternatives --set javac $JAVAC_13
   ```

## Appendix GG: Destroy Jenkins agent

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Change to the shared manegment environment

   ```ShellSession
   $ cd terraform/environments/ab2d-mgmt-east-dev-shared
   ```

1. Destroy the Jenkins agent

   ```ShellSession
   $ terraform destroy \
     --target module.jenkins_agent \
     --auto-approve
   ```

## Appendix HH: Manual test of Splunk configuration

### Prepare IAM policies and roles for CloudWatch Log groups

1. Note that the following IAM policy has been created in Mgmt, Dev, Sbx, and Impl

   **Policy:** Ab2dCloudWatchLogsPolicy

   ```
   {
       "Version": "2012-10-17",
       "Statement": [
           {
               "Sid": "Stmt1584652230001",
               "Effect": "Allow",
               "Action": [
                   "logs:CreateLogGroup",
                   "logs:CreateLogStream",
                   "logs:PutLogEvents",
		   "logs:DescribeLogGroups",
                   "logs:DescribeLogStreams"
               ],
               "Resource": [
                   "arn:aws:logs:*:*:*"
               ]
           }
       ]
   }
   ```

1. Note that the "Ab2dCloudWatchLogsPolicy" IAM policy has been attached to the "Ab2dInstanceRole" role in Mgmt, Dev, Sbx, and Impl

1. Set trust relationship between the "Ab2dInstanceRole" role and the VPC flow log service

   1. Select the following IAM role

      ```
      Ab2dInstanceRole
      ```

   1. Select the **Trust relationships** tab

   1. Select **Edit trust relationship**

   1. Modify the trust relationship to include the VPC flow logs service

      *Example:*

      ```
      {
        "Version": "2012-10-17",
        "Statement": [
          {
            "Sid": "",
            "Effect": "Allow",
            "Principal": {
              "Service": [
                "ecs-tasks.amazonaws.com",
                "lambda.amazonaws.com",
                "ec2.amazonaws.com",
		"vpc-flow-logs.amazonaws.com"
              ]
            },
            "Action": "sts:AssumeRole"
          }
        ]
      }
      ```

   1. Select **Update Trust Policy**

### Configure CloudWatch Log groups

#### Configure CloudWatch Log groups for management environment

##### Configure CloudTrail CloudWatch Log group for management environment

1. Log on to the AWS management account

1. Create a CloudTrail CloudWatch Log group

   1. Select **Log groups** from the leftmost panel

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log Group Name:** cloudtrail-logs

   1. Select **Create log group**

1. Create a trail in CloudTrail

   1. Select **CloudTrail**

   1. Select **Create trail**

   1. Configure "Create Trail"

      - **Trail name:** cloudtrail-default

      - **Apply trail to all regions:** No

   1. Configure "Management events"

      - **Read/Write events:** All

      - **Log AWS KMS events:** Yes

   1. Configure "Insights events"

      - **Log Insights events:** No

   1. Configure "Data Events" for the "S3" tab

      - **Select all S3 buckets in your account:** Checked

   1. Configure "Storage location"

      - **Create a new S3 bucket:** No

      - **S3 bucket:** ab2d-mgmt-east-dev-cloudtrail

   1. Select **Create**

1. Create a role for CloudTrail

   1. Set the AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Change to the "Deploy" directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy
      ```

   1. Change to the shared directory

      ```ShellSession
      $ cd terraform/environments/ab2d-mgmt-east-dev-shared
      ```

   1. Create the "Ab2dCloudTrailAssumeRole" role

      ```ShellSession
      $ aws --region us-east-1 iam create-role \
        --role-name Ab2dCloudTrailAssumeRole \
        --assume-role-policy-document file://ab2d-cloudtrail-assume-role-policy.json
      ```

   1. Add a CloudWatch log group policy to the CloudTrail role

      ```ShellSession
      $ aws --region us-east-1 iam put-role-policy \
        --role-name Ab2dCloudTrailAssumeRole \
	--policy-name Ab2dCloudTrailPolicy \
	--policy-document file://ab2d-cloudtrail-cloudwatch-policy.json
      ```

1. Update the trail in CloudTrail with the log group and role information

   ```ShellSession
   $ aws --region us-east-1 cloudtrail update-trail \
     --name cloudtrail-default \
     --cloud-watch-logs-log-group-arn arn:aws:logs:us-east-1:653916833532:log-group:cloudtrail-logs:* \
     --cloud-watch-logs-role-arn arn:aws:iam::653916833532:role/Ab2dCloudTrailAssumeRole
   ```

##### Configure VPC flow log CloudWatch Log group for management environment

1. Log on to the AWS management account

1. Create a VPC flow log CloudWatch Log group

   1. Select **CloudWatch**

   1. Select **Log groups** from the leftmost panel

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log Group Name:** vpc-flowlogs

   1. Select **Create log group**

1. Create a VPC flow log

   1. Select **VPC**

   1. Select **Your VPCs** from the leftmost panel

   1. Select the following VPC

      ```
      ab2d-mgmt-east-dev
      ```

   1. Select the **Flow Logs** tab

   1. Select **Create flow log**

   1. Configure the "Create flow log" page

      *Format:*

      - **Filter:** All

      - **Maximum aggregation interval:** 10 minutes

      - **Destination:** Send to CloudWatch Logs

      - **Destination log group:** vpc-flowlogs

      - **IAM role:** Ab2dInstanceRole

   1. Select **Create**

   1. Select **Close** on the "Create flow log" page

##### Onboard first Jenkins master log to CloudWatch Log groups

1. Connect to Jenkins master

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      *Example for Mgmt environment:*

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Connect to the development controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

1. Switch to the root user

   ```ShellSession
   $ sudo su
   ```

1. Verify existing logs prior to changes

   OS Level Logging                        |Exists|CloudWatch Log Group
   ----------------------------------------|------|------------------------------------------------
   /var/log/amazon/ssm/amazon-ssm-agent.log|yes   |/aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log
   /var/log/audit/audit.log                |yes   |/aws/ec2/var/log/audit/audit.log
   /var/log/awslogs.log                    |no    |/aws/ec2/var/log/awslogs.log
   /var/log/cloud-init-output.log          |yes   |/aws/ec2/var/log/cloud-init-output.log
   /var/log/cloud-init.log                 |yes   |/aws/ec2/var/log/cloud-init.log
   /var/log/cron                           |yes   |/aws/ec2/var/log/cron
   /var/log/dmesg                          |yes   |/aws/ec2/var/log/dmesg
   /var/log/maillog                        |yes   |/aws/ec2/var/log/maillog
   /var/log/messages                       |yes   |/aws/ec2/var/log/messages
   /var/log/secure                         |yes   |/aws/ec2/var/log/secure
   /var/opt/ds_agent/diag/ds_agent-err.log |yes   |/aws/ec2/var/opt/ds_agent/diag/ds_agent-err.log
   /var/opt/ds_agent/diag/ds_agent.log     |yes   |/aws/ec2/var/opt/ds_agent/diag/ds_agent.log
   /var/opt/ds_agent/diag/ds_am.log        |yes   |/aws/ec2/var/opt/ds_agent/diag/ds_am.log
   N/A                                     |N/A   |cloudtrail-logs
   N/A                                     |N/A   |vpc-flowlogs

1. Exit the root user

   ```ShellSession
   $ exit
   ```

1. Download the CloudWatch Log agent

   1. Change to the "/tmp" directory

      ```ShellSession
      $ cd /tmp
      ```

   1. Download the CloudWatch Log Agent

      ```ShellSession
      $ curl -O https://s3.amazonaws.com/aws-cloudwatch/downloads/latest/awslogs-agent-setup.py
      ```

1. Configure the CloudWatch Log Agent and start logging "/var/log/messages"

   1. Enter the following

      ```ShellSession
      $ sudo python /tmp/awslogs-agent-setup.py --region us-east-1
      ```

   1. Note the following dependencies are downloaded

      ```
      AgentDependencies/
      AgentDependencies/awslogscli/
      AgentDependencies/awslogscli/urllib3-1.25.6.tar.gz
      AgentDependencies/awslogscli/jmespath-0.9.2.tar.gz
      AgentDependencies/awslogscli/colorama-0.3.7.zip
      AgentDependencies/awslogscli/idna-2.8.tar.gz
      AgentDependencies/awslogscli/awscli-1.11.41.tar.gz
      AgentDependencies/awslogscli/argparse-1.2.1.tar.gz
      AgentDependencies/awslogscli/botocore-1.13.9.tar.gz
      AgentDependencies/awslogscli/docutils-0.15.2.tar.gz
      AgentDependencies/awslogscli/pyasn1-0.2.3.tar.gz
      AgentDependencies/awslogscli/python-dateutil-2.6.0.tar.gz
      AgentDependencies/awslogscli/botocore-1.5.4.tar.gz
      AgentDependencies/awslogscli/jmespath-0.9.4.tar.gz
      AgentDependencies/awslogscli/awscli-cwlogs-1.4.6.tar.gz
      AgentDependencies/awslogscli/ordereddict-1.1.tar.gz
      AgentDependencies/awslogscli/futures-3.3.0.tar.gz
      AgentDependencies/awslogscli/futures-3.0.5.tar.gz
      AgentDependencies/awslogscli/certifi-2019.9.11.tar.gz
      AgentDependencies/awslogscli/six-1.12.0.tar.gz
      AgentDependencies/awslogscli/s3transfer-0.1.10.tar.gz
      AgentDependencies/awslogscli/six-1.10.0.tar.gz
      AgentDependencies/awslogscli/requests-2.18.4.tar.gz
      AgentDependencies/awslogscli/rsa-3.4.2.tar.gz
      AgentDependencies/awslogscli/s3transfer-0.2.1.tar.gz
      AgentDependencies/awslogscli/docutils-0.13.1.tar.gz
      AgentDependencies/awslogscli/urllib3-1.22.tar.gz
      AgentDependencies/awslogscli/awscli-1.16.273.tar.gz
      AgentDependencies/awslogscli/PyYAML-5.1.2.tar.gz
      AgentDependencies/awslogscli/idna-2.5.tar.gz
      AgentDependencies/awslogscli/PyYAML-3.12.tar.gz
      AgentDependencies/awslogscli/pyasn1-0.4.7.tar.gz
      AgentDependencies/awslogscli/colorama-0.4.1.tar.gz
      AgentDependencies/awslogscli/simplejson-3.3.0.tar.gz
      AgentDependencies/awslogscli/chardet-3.0.4.tar.gz
      AgentDependencies/virtualenv-15.1.0/
      AgentDependencies/virtualenv-15.1.0/setup.cfg
      AgentDependencies/virtualenv-15.1.0/tests/
      AgentDependencies/virtualenv-15.1.0/tests/__init__.py
      AgentDependencies/virtualenv-15.1.0/tests/test_cmdline.py
      AgentDependencies/virtualenv-15.1.0/tests/test_virtualenv.py
      AgentDependencies/virtualenv-15.1.0/tests/test_activate_output.expected
      AgentDependencies/virtualenv-15.1.0/tests/test_activate.sh
      AgentDependencies/virtualenv-15.1.0/scripts/
      AgentDependencies/virtualenv-15.1.0/scripts/virtualenv
      AgentDependencies/virtualenv-15.1.0/virtualenv.py
      AgentDependencies/virtualenv-15.1.0/MANIFEST.in
      AgentDependencies/virtualenv-15.1.0/README.rst
      AgentDependencies/virtualenv-15.1.0/AUTHORS.txt
      AgentDependencies/virtualenv-15.1.0/setup.py
      AgentDependencies/virtualenv-15.1.0/LICENSE.txt
      AgentDependencies/virtualenv-15.1.0/virtualenv_support/
      AgentDependencies/virtualenv-15.1.0/virtualenv_support/__init__.py
      AgentDependencies/virtualenv-15.1.0/virtualenv_support/wheel-0.29.0-py2.py3-none-any.whl
      AgentDependencies/virtualenv-15.1.0/virtualenv_support/argparse-1.4.0-py2.py3-none-any.whl
      AgentDependencies/virtualenv-15.1.0/virtualenv_support/pip-9.0.1-py2.py3-none-any.whl
      AgentDependencies/virtualenv-15.1.0/virtualenv_support/setuptools-28.8.0-py2.py3-none-any.whl
      AgentDependencies/virtualenv-15.1.0/PKG-INFO
      AgentDependencies/virtualenv-15.1.0/bin/
      AgentDependencies/virtualenv-15.1.0/bin/rebuild-script.py
      AgentDependencies/virtualenv-15.1.0/virtualenv.egg-info/
      AgentDependencies/virtualenv-15.1.0/virtualenv.egg-info/not-zip-safe
      AgentDependencies/virtualenv-15.1.0/virtualenv.egg-info/SOURCES.txt
      AgentDependencies/virtualenv-15.1.0/virtualenv.egg-info/entry_points.txt
      AgentDependencies/virtualenv-15.1.0/virtualenv.egg-info/top_level.txt
      AgentDependencies/virtualenv-15.1.0/virtualenv.egg-info/PKG-INFO
      AgentDependencies/virtualenv-15.1.0/virtualenv.egg-info/dependency_links.txt
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/activate.ps1
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/activate.csh
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/deactivate.bat
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/activate_this.py
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/activate.fish
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/python-config
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/activate.bat
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/distutils.cfg
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/site.py
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/activate.sh
      AgentDependencies/virtualenv-15.1.0/virtualenv_embedded/distutils-init.py
      AgentDependencies/virtualenv-15.1.0/docs/
      AgentDependencies/virtualenv-15.1.0/docs/userguide.rst
      AgentDependencies/virtualenv-15.1.0/docs/index.rst
      AgentDependencies/virtualenv-15.1.0/docs/development.rst
      AgentDependencies/virtualenv-15.1.0/docs/reference.rst
      AgentDependencies/virtualenv-15.1.0/docs/Makefile
      AgentDependencies/virtualenv-15.1.0/docs/conf.py
      AgentDependencies/virtualenv-15.1.0/docs/changes.rst
      AgentDependencies/virtualenv-15.1.0/docs/installation.rst
      AgentDependencies/virtualenv-15.1.0/docs/make.bat
      AgentDependencies/pip-6.1.1.tar.gz
      ```

   1. Wait for the following to display

      ```
      Step 1 of 5: Installing pip ...DONE
      ```

   1. Wait for the following to display

      *Note that this may take a while.*

      ```
      Step 2 of 5: Downloading the latest CloudWatch Logs agent bits ...DONE
      ```

   1. Note that the following is displayed

      ```
      Step 3 of 5: Configuring AWS CLI ...
      ```

   1. Press **return** on the keyboard to accept the default at the "AWS Access Key ID" prompt

   1. Press **return** on the keyboard to accept the default at the "AWS Secret Access Key" prompt

   1. Press **return** on the keyboard to accept the default at the "Default region name" prompt

   1. Press **return** on the keyboard to accept the default at the "Default output format" prompt

   1. Note that the following is displayed

      ```
      Step 4 of 5: Configuring the CloudWatch Logs Agent ...
      ```

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/messages

      - **Destination Log Group name:** /aws/ec2/var/log/messages

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** N

   1. Wait for the following to display

      ```
      Step 5 of 5: Setting up agent as a daemon ...DONE
      ```

   1. Note the following output

      ------------------------------------------------------
      - Configuration file successfully saved at: /var/awslogs/etc/awslogs.conf
      - You can begin accessing new log events after a few moments at https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logs:
      - You can use 'sudo service awslogs start|stop|status|restart' to control the daemon.
      - To see diagnostic information for the CloudWatch Logs Agent, see /var/log/awslogs.log
      - You can rerun interactive setup using 'sudo python ./awslogs-agent-setup.py --region us-east-1 --only-generate-config'
      ------------------------------------------------------

1. View the "/var/log/messages" entry in the newly created CloudWatch Log agent

   1. Enter the following

      ```ShellSession
      $ sudo cat /var/awslogs/etc/awslogs.conf | tail -7
      ```

   1. Note the output

      ```
      [/var/log/messages]
      datetime_format = %Y-%m-%d %H:%M:%S
      file = /var/log/messages
      buffer_duration = 5000
      log_stream_name = {instance_id}
      initial_position = start_of_file
      log_group_name = /aws/ec2/var/log/messages
      ```

1. Ensure the CloudWatch Log Agent is running

   1. Check the status of the CloudWatch Log Agent

      ```ShellSession
      $ service awslogs status
      ```

   1. If the CloudWatch Log Agent is not running, start it by entering the following

      ```ShellSession
      $ sudo service awslogs start
      ```

1. Exit Jenkins master

   ```ShellSession
   $ exit
   ```

##### Onboard additional CloudWatch log groups for Jenkins master

1. Connect to Jenkins master

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      *Example for Mgmt environment:*

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Connect to the development controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

1. Onboard additional log groups

   1. Change to the "/tmp" directory

      ```ShellSession
      $ cd /tmp
      ```

   1. Run the interactive setup for the CloudWatch Log agent

      ```ShellSession
      $ sudo python ./awslogs-agent-setup.py \
        --region us-east-1 \
        --only-generate-config
      ```

   1. Note that the following is displayed

      ```
      Step 3 of 5: Configuring AWS CLI ...
      ```

   1. Press **return** on the keyboard to accept the default at the "AWS Access Key ID" prompt

   1. Press **return** on the keyboard to accept the default at the "AWS Secret Access Key" prompt

   1. Press **return** on the keyboard to accept the default at the "Default region name" prompt

   1. Press **return** on the keyboard to accept the default at the "Default output format" prompt

   1. Note that the following is displayed

      ```
      Step 4 of 5: Configuring the CloudWatch Logs Agent ...
      ```

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/amazon/ssm/amazon-ssm-agent.log

      - **Destination Log Group name:** /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/audit/audit.log

      - **Destination Log Group name:** /aws/ec2/var/log/audit/audit.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/awslogs.log

      - **Destination Log Group name:** /aws/ec2/var/log/awslogs.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cloud-init-output.log

      - **Destination Log Group name:** /aws/ec2/var/log/cloud-init-output.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cloud-init.log

      - **Destination Log Group name:** /aws/ec2/var/log/cloud-init.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cron

      - **Destination Log Group name:** /aws/ec2/var/log/cron

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/dmesg

      - **Destination Log Group name:** /aws/ec2/var/log/dmesg

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/maillog

      - **Destination Log Group name:** /aws/ec2/var/log/maillog

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/secure

      - **Destination Log Group name:** /aws/ec2/var/log/secure

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_agent-err.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_agent-err.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_agent.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_agent.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_am.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_am.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** N

   1. Note all the new CloudWatch log groups configurations are appended after the first "/var/log/messages" section in the configuaration file

      ```ShellSession
      $ sudo cat /var/awslogs/etc/awslogs.conf
      ```

1. Restart the awslogs service

   ```ShellSession
   $ sudo service awslogs restart
   ```

1. Reload systemd configuration

   ```ShellSession
   $ sudo systemctl daemon-reload
   ```

1. Exit Jenkins master

   ```ShellSession
   $ exit
   ```

1. Verify that all expected CloudWatch Log groups are present

   1. Log on to the AWS develoment environment account

   1. Select **CloudWatch**

   1. Select **Log groups** from the leftmost panel

   1. Verify that all of the following log groups are displayed

      - /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - /aws/ec2/var/log/audit/audit.log

      - /aws/ec2/var/log/awslogs.log

      - /aws/ec2/var/log/cloud-init-output.log

      - /aws/ec2/var/log/cloud-init.log

      - /aws/ec2/var/log/cron

      - /aws/ec2/var/log/dmesg

      - /aws/ec2/var/log/messages

      - /aws/ec2/var/log/secure

      - /aws/ec2/var/opt/ds_agent/diag/ds_agent-err.log

      - /aws/ec2/var/opt/ds_agent/diag/ds_agent.log

      - /aws/ec2/var/opt/ds_agent/diag/ds_am.log

      - vpc-flowlogs

      - cloudtrail-logs

1. If any of the log groups are missing, do the following:

   1. Note the missing group(s) so that you can later investigate why it is missing

      *Noted missing groups when creating on 2020-04-07:*

      - NONE

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log group name:** {missing log group}

   1. Select **Create log group**

   1. Repeat this process for all missing groups

1. Select each log group and note which ones have no data

   *Noted log groups with no data when checking on 2020-04-07:*

   - NONE

##### Onboard first Jenkins agent log to CloudWatch Log groups

1. Connect to Jenkins agent through the Jenkins master using the ProxyJump flag (-J)

   1. Ensure that you are connected to the Cisco VPN

   1. Set the management AWS agent

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Get the public IP address of Jenkins master instance

      ```ShellSession
      $ JENKINS_MASTER_PUBLIC_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PublicIpAddress" \
        --output text)
      ```

   1. Get the private IP address of Jenkins agent instance

      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the Jenkins agent through the Jenkins master using the ProxyJump flag (-J)

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem -J \
        ec2-user@$JENKINS_MASTER_PUBLIC_IP \
	ec2-user@$JENKINS_AGENT_PRIVATE_IP
      ```

1. Download the CloudWatch Log agent

   1. Change to the "/tmp" directory

      ```ShellSession
      $ cd /tmp
      ```

   1. Download the CloudWatch Log Agent

      ```ShellSession
      $ curl -O https://s3.amazonaws.com/aws-cloudwatch/downloads/latest/awslogs-agent-setup.py
      ```

1. Configure the CloudWatch Log Agent and start logging "/var/log/messages"

   1. Enter the following

      ```ShellSession
      $ sudo python /tmp/awslogs-agent-setup.py --region us-east-1
      ```

   1. Wait for the following to display

      ```
      Step 1 of 5: Installing pip ...DONE
      ```

   1. Wait for the following to display

      *Note that this may take a while.*

      ```
      Step 2 of 5: Downloading the latest CloudWatch Logs agent bits ...DONE
      ```

   1. Note that the following is displayed

      ```
      Step 3 of 5: Configuring AWS CLI ...
      ```

   1. Press **return** on the keyboard to accept the default at the "AWS Access Key ID" prompt

   1. Press **return** on the keyboard to accept the default at the "AWS Secret Access Key" prompt

   1. Press **return** on the keyboard to accept the default at the "Default region name" prompt

   1. Press **return** on the keyboard to accept the default at the "Default output format" prompt

   1. Note that the following is displayed

      ```
      Step 4 of 5: Configuring the CloudWatch Logs Agent ...
      ```

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/messages

      - **Destination Log Group name:** /aws/ec2/var/log/messages

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** N

   1. Wait for the following to display

      ```
      Step 5 of 5: Setting up agent as a daemon ...DONE
      ```

   1. Note the following output

      ------------------------------------------------------
      - Configuration file successfully saved at: /var/awslogs/etc/awslogs.conf
      - You can begin accessing new log events after a few moments at https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logs:
      - You can use 'sudo service awslogs start|stop|status|restart' to control the daemon.
      - To see diagnostic information for the CloudWatch Logs Agent, see /var/log/awslogs.log
      - You can rerun interactive setup using 'sudo python ./awslogs-agent-setup.py --region us-east-1 --only-generate-config'
      ------------------------------------------------------

1. View the "/var/log/messages" entry in the newly created CloudWatch Log agent

   1. Enter the following

      ```ShellSession
      $ sudo cat /var/awslogs/etc/awslogs.conf | tail -7
      ```

   1. Note the output

      ```
      [/var/log/messages]
      datetime_format = %Y-%m-%d %H:%M:%S
      file = /var/log/messages
      buffer_duration = 5000
      log_stream_name = {instance_id}
      initial_position = start_of_file
      log_group_name = /aws/ec2/var/log/messages
      ```

1. Ensure the CloudWatch Log Agent is running

   1. Check the status of the CloudWatch Log Agent

      ```ShellSession
      $ service awslogs status
      ```

   1. If the CloudWatch Log Agent is not running, start it by entering the following

      ```ShellSession
      $ sudo service awslogs start
      ```

1. Exit Jenkins agent

   ```ShellSession
   $ exit
   ```

##### Onboard additional CloudWatch log groups for Jenkins agent

1. Connect to Jenkins agent

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      *Example for Mgmt environment:*

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Connect to the development controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

1. Onboard additional log groups

   1. Change to the "/tmp" directory

      ```ShellSession
      $ cd /tmp
      ```

   1. Run the interactive setup for the CloudWatch Log agent

      ```ShellSession
      $ sudo python ./awslogs-agent-setup.py \
        --region us-east-1 \
        --only-generate-config
      ```

   1. Note that the following is displayed

      ```
      Step 3 of 5: Configuring AWS CLI ...
      ```

   1. Press **return** on the keyboard to accept the default at the "AWS Access Key ID" prompt

   1. Press **return** on the keyboard to accept the default at the "AWS Secret Access Key" prompt

   1. Press **return** on the keyboard to accept the default at the "Default region name" prompt

   1. Press **return** on the keyboard to accept the default at the "Default output format" prompt

   1. Note that the following is displayed

      ```
      Step 4 of 5: Configuring the CloudWatch Logs Agent ...
      ```

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/amazon/ssm/amazon-ssm-agent.log

      - **Destination Log Group name:** /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/audit/audit.log

      - **Destination Log Group name:** /aws/ec2/var/log/audit/audit.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/awslogs.log

      - **Destination Log Group name:** /aws/ec2/var/log/awslogs.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cloud-init-output.log

      - **Destination Log Group name:** /aws/ec2/var/log/cloud-init-output.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cloud-init.log

      - **Destination Log Group name:** /aws/ec2/var/log/cloud-init.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cron

      - **Destination Log Group name:** /aws/ec2/var/log/cron

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/dmesg

      - **Destination Log Group name:** /aws/ec2/var/log/dmesg

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/maillog

      - **Destination Log Group name:** /aws/ec2/var/log/maillog

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/secure

      - **Destination Log Group name:** /aws/ec2/var/log/secure

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_agent-err.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_agent-err.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_agent.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_agent.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_am.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_am.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** N

   1. Note all the new CloudWatch log groups configurations are appended after the first "/var/log/messages" section in the configuaration file

      ```ShellSession
      $ sudo cat /var/awslogs/etc/awslogs.conf
      ```

1. Restart the awslogs service

   ```ShellSession
   $ sudo service awslogs restart
   ```

1. Reload systemd configuration

   ```ShellSession
   $ sudo systemctl daemon-reload
   ```

1. Exit Jenkins agent

   ```ShellSession
   $ exit
   ```

##### Verify logging to CloudWatch Log Group for management environment

1. Open Chrome

1. Log on to the development AWS account

1. Select **CloudWatch**

1. Select **Log groups** under the "Logs" section in the leftmost panel

1. Select the following

   ```
   /aws/ec2/var/log/messages
   ```

1. Select the instance id

   *Example:*

   ```
   i-01fbbfdf09d80d874
   ```

1. Note the list of events that appear within the main page

#### Configure CloudWatch Log groups for development environment

##### Configure CloudTrail CloudWatch Log group for development environment

1. Log on to the AWS development account

1. Create a CloudTrail CloudWatch Log group

   1. Select **Log groups** from the leftmost panel

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log Group Name:** cloudtrail-logs

   1. Select **Create log group**

1. Create a trail in CloudTrail

   1. Select **CloudTrail**

   1. Select **Create trail**

   1. Configure "Create Trail"

      - **Trail name:** cloudtrail-default

      - **Apply trail to all regions:** No

   1. Configure "Management events"

      - **Read/Write events:** All

      - **Log AWS KMS events:** Yes

   1. Configure "Insights events"

      - **Log Insights events:** No

   1. Configure "Data Events" for the "S3" tab

      - **Select all S3 buckets in your account:** Checked

   1. Configure "Storage location"

      - **Create a new S3 bucket:** No

      - **S3 bucket:** ab2d-dev-cloudtrail

   1. Select **Create**

1. Create a role for CloudTrail

   1. Set the AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      ```

   1. Change to the "Deploy" directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy
      ```

   1. Change to the shared directory

      ```ShellSession
      $ cd terraform/environments/ab2d-dev-shared
      ```

   1. Create the "Ab2dCloudTrailAssumeRole" role

      ```ShellSession
      $ aws --region us-east-1 iam create-role \
        --role-name Ab2dCloudTrailAssumeRole \
        --assume-role-policy-document file://ab2d-cloudtrail-assume-role-policy.json
      ```

   1. Add a CloudWatch log group policy to the CloudTrail role

      ```ShellSession
      $ aws --region us-east-1 iam put-role-policy \
        --role-name Ab2dCloudTrailAssumeRole \
	--policy-name Ab2dCloudTrailPolicy \
	--policy-document file://ab2d-cloudtrail-cloudwatch-policy.json
      ```

1. Update the trail in CloudTrail with the log group and role information

   ```ShellSession
   $ aws --region us-east-1 cloudtrail update-trail \
     --name cloudtrail-default \
     --cloud-watch-logs-log-group-arn arn:aws:logs:us-east-1:349849222861:log-group:cloudtrail-logs:* \
     --cloud-watch-logs-role-arn arn:aws:iam::349849222861:role/Ab2dCloudTrailAssumeRole
   ```

##### Configure VPC flow log CloudWatch Log group for development environment

1. Log on to the AWS development account

1. Create a VPC flow log CloudWatch Log group

   1. Select **CloudWatch**

   1. Select **Log groups** from the leftmost panel

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log Group Name:** vpc-flowlogs

   1. Select **Create log group**

1. Create a VPC flow log

   1. Select **VPC**

   1. Select **Your VPCs** from the leftmost panel

   1. Select the following VPC

      ```
      ab2d-dev
      ```

   1. Select the **Flow Logs** tab

   1. Select **Create flow log**

   1. Configure the "Create flow log" page

      *Format:*

      - **Filter:** All

      - **Maximum aggregation interval:** 10 minutes

      - **Destination:** Send to CloudWatch Logs

      - **Destination log group:** vpc-flowlogs

      - **IAM role:** Ab2dInstanceRole

   1. Select **Create**

   1. Select **Close** on the "Create flow log" page

##### Onboard first deployment controller log to CloudWatch Log groups for development environment

1. Connect to an API node in development

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      ```

   1. Connect to the deployment controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

1. Download the CloudWatch Log agent

   1. Change to the "/tmp" directory

      ```ShellSession
      $ cd /tmp
      ```

   1. Download the CloudWatch Log Agent

      ```ShellSession
      $ curl -O https://s3.amazonaws.com/aws-cloudwatch/downloads/latest/awslogs-agent-setup.py
      ```

1. Configure the CloudWatch Log Agent and start logging "/var/log/messages"

   1. Enter the following

      ```ShellSession
      $ sudo python /tmp/awslogs-agent-setup.py --region us-east-1
      ```

   1. Wait for the following to display

      ```
      Step 1 of 5: Installing pip ...DONE
      ```

   1. Wait for the following to display

      *Note that this may take a while.*

      ```
      Step 2 of 5: Downloading the latest CloudWatch Logs agent bits ...DONE
      ```

   1. Note that the following is displayed

      ```
      Step 3 of 5: Configuring AWS CLI ...
      ```

   1. Press **return** on the keyboard to accept the default at the "AWS Access Key ID" prompt

   1. Press **return** on the keyboard to accept the default at the "AWS Secret Access Key" prompt

   1. Press **return** on the keyboard to accept the default at the "Default region name" prompt

   1. Press **return** on the keyboard to accept the default at the "Default output format" prompt

   1. Note that the following is displayed

      ```
      Step 4 of 5: Configuring the CloudWatch Logs Agent ...
      ```

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/messages

      - **Destination Log Group name:** /aws/ec2/var/log/messages

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** N

   1. Wait for the following to display

      ```
      Step 5 of 5: Setting up agent as a daemon ...DONE
      ```

   1. Note the following output

      ------------------------------------------------------
      - Configuration file successfully saved at: /var/awslogs/etc/awslogs.conf
      - You can begin accessing new log events after a few moments at https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logs:
      - You can use 'sudo service awslogs start|stop|status|restart' to control the daemon.
      - To see diagnostic information for the CloudWatch Logs Agent, see /var/log/awslogs.log
      - You can rerun interactive setup using 'sudo python ./awslogs-agent-setup.py --region us-east-1 --only-generate-config'
      ------------------------------------------------------

1. View the "/var/log/messages" entry in the newly created CloudWatch Log agent

   1. Enter the following
   
      ```ShellSession
      $ sudo cat /var/awslogs/etc/awslogs.conf | tail -7
      ```

   1. Note the output

      ```
      [/var/log/messages]
      datetime_format = %Y-%m-%d %H:%M:%S
      file = /var/log/messages
      buffer_duration = 5000
      log_stream_name = {instance_id}
      initial_position = start_of_file
      log_group_name = /aws/ec2/var/log/messages
      ```

1. Ensure the CloudWatch Log Agent is running

   1. Check the status of the CloudWatch Log Agent

      ```ShellSession
      $ service awslogs status
      ```

   1. If the CloudWatch Log Agent is not running, start it by entering the following

      ```ShellSession
      $ sudo service awslogs start
      ```

1. Exit the controler

   ```ShellSession
   $ exit
   ```

##### Onboard additional CloudWatch log groups for deployment controller log for development environment

1. Connect to deployment controller

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      *Example for Mgmt environment:*

      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      ```

   1. Connect to the development controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

1. Onboard additional log groups

   1. Change to the "/tmp" directory

      ```ShellSession
      $ cd /tmp
      ```

   1. Run the interactive setup for the CloudWatch Log agent

      ```ShellSession
      $ sudo python ./awslogs-agent-setup.py \
        --region us-east-1 \
        --only-generate-config
      ```

   1. Note that the following is displayed

      ```
      Step 3 of 5: Configuring AWS CLI ...
      ```

   1. Press **return** on the keyboard to accept the default at the "AWS Access Key ID" prompt

   1. Press **return** on the keyboard to accept the default at the "AWS Secret Access Key" prompt

   1. Press **return** on the keyboard to accept the default at the "Default region name" prompt

   1. Press **return** on the keyboard to accept the default at the "Default output format" prompt

   1. Note that the following is displayed

      ```
      Step 4 of 5: Configuring the CloudWatch Logs Agent ...
      ```

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/amazon/ssm/amazon-ssm-agent.log

      - **Destination Log Group name:** /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/audit/audit.log

      - **Destination Log Group name:** /aws/ec2/var/log/audit/audit.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/awslogs.log

      - **Destination Log Group name:** /aws/ec2/var/log/awslogs.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cloud-init-output.log

      - **Destination Log Group name:** /aws/ec2/var/log/cloud-init-output.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cloud-init.log

      - **Destination Log Group name:** /aws/ec2/var/log/cloud-init.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cron

      - **Destination Log Group name:** /aws/ec2/var/log/cron

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/dmesg

      - **Destination Log Group name:** /aws/ec2/var/log/dmesg

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/maillog

      - **Destination Log Group name:** /aws/ec2/var/log/maillog

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/secure

      - **Destination Log Group name:** /aws/ec2/var/log/secure

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_agent-err.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_agent-err.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_agent.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_agent.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_am.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_am.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** N

   1. Note all the new CloudWatch log groups configurations are appended after the first "/var/log/messages" section in the configuaration file

      ```ShellSession
      $ sudo cat /var/awslogs/etc/awslogs.conf
      ```

1. Restart the awslogs service

   ```ShellSession
   $ sudo service awslogs restart
   ```

1. Reload systemd configuration

   ```ShellSession
   $ sudo systemctl daemon-reload
   ```

1. Exit deployment controller

   ```ShellSession
   $ exit
   ```

1. Verify that all expected CloudWatch Log groups are present

   1. Log on to the AWS develoment environment account

   1. Select **CloudWatch**

   1. Select **Log groups** from the leftmost panel

   1. Verify that all of the following log groups are displayed
	
      - /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - /aws/ec2/var/log/audit/audit.log

      - /aws/ec2/var/log/awslogs.log

      - /aws/ec2/var/log/cloud-init-output.log

      - /aws/ec2/var/log/cloud-init.log

      - /aws/ec2/var/log/cron

      - /aws/ec2/var/log/dmesg

      - /aws/ec2/var/log/messages

      - /aws/ec2/var/log/secure

      - /aws/ec2/var/opt/ds_agent/diag/ds_agent-err.log

      - /aws/ec2/var/opt/ds_agent/diag/ds_agent.log

      - /aws/ec2/var/opt/ds_agent/diag/ds_am.log

      - vpc-flowlogs

      - cloudtrail-logs

1. If any of the log groups are missing, do the following:

   1. Note the missing group(s) so that you can later investigate why it is missing

      *Noted missing groups when creating on 2020-04-07:*

      - /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - /aws/ec2/var/log/cloud-init.log

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log group name:** {missing log group}

   1. Select **Create log group**

   1. Repeat this process for all missing groups

1. Select each log group and note which ones have no data

   *Noted log groups with no data when checking on 2020-04-07:*

   - /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

   - /aws/ec2/var/log/cloud-init.log

   - vpc-flowlogs

##### Onboard first api node log to CloudWatch Log groups for development environment

> *** TO DO ***

1. Connect to an API node in development

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      ```

   1. Connect to the development controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Connect to the desired API node

      *Example for connecting to the first API node:*

      ```ShellSession
      $ ssh ec2-user@$(./list-api-instances.sh \
        | grep 10. \
        | awk '{print $2}' \
        | head -n 1)
      ```

> *** TO DO ***

##### Onboard additional CloudWatch log groups for first api node log for development environment

> *** TO DO ***

1. Connect to an API node in sandbox

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      ```

   1. Connect to the sandbox controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Connect to the desired API node

      *Example for connecting to the first API node:*

      ```ShellSession
      $ ssh ec2-user@$(./list-api-instances.sh \
        | grep 10. \
        | awk '{print $2}' \
        | head -n 1)
      ```

> *** TO DO ***

#### Configure CloudWatch Log groups for sandbox environment

##### Configure CloudTrail CloudWatch Log group for sandbox environment

1. Note that I am skipping this section since Splunk team already created a CloudTrail

> *** TO DO ***: Verify this with Splunk team

1. Log on to the AWS sandbox account

1. Create a CloudTrail CloudWatch Log group

   1. Select **Log groups** from the leftmost panel

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log Group Name:** cloudtrail-logs

   1. Select **Create log group**

1. Create a trail in CloudTrail

   1. Select **CloudTrail**

   1. Select **Create trail**

   1. Configure "Create Trail"

      - **Trail name:** cloudtrail-default

      - **Apply trail to all regions:** No

   1. Configure "Management events"

      - **Read/Write events:** All

      - **Log AWS KMS events:** Yes

   1. Configure "Insights events"

      - **Log Insights events:** No

   1. Configure "Data Events" for the "S3" tab

      - **Select all S3 buckets in your account:** Checked

   1. Configure "Storage location"

      - **Create a new S3 bucket:** No

      - **S3 bucket:** ab2d-sbx-sandbox-cloudtrail

   1. Select **Create**

1. Create a role for CloudTrail

   1. Set the AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-sbx-sandbox
      ```

   1. Change to the "Deploy" directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy
      ```

   1. Change to the shared directory

      ```ShellSession
      $ cd terraform/environments/ab2d-sbx-sandbox-shared
      ```

   1. Create the "Ab2dCloudTrailAssumeRole" role

      ```ShellSession
      $ aws --region us-east-1 iam create-role \
        --role-name Ab2dCloudTrailAssumeRole \
        --assume-role-policy-document file://ab2d-cloudtrail-assume-role-policy.json
      ```

   1. Add a CloudWatch log group policy to the CloudTrail role

      ```ShellSession
      $ aws --region us-east-1 iam put-role-policy \
        --role-name Ab2dCloudTrailAssumeRole \
	--policy-name Ab2dCloudTrailPolicy \
	--policy-document file://ab2d-cloudtrail-cloudwatch-policy.json
      ```

1. Update the trail in CloudTrail with the log group and role information

   ```ShellSession
   $ aws --region us-east-1 cloudtrail update-trail \
     --name cloudtrail-default \
     --cloud-watch-logs-log-group-arn arn:aws:logs:us-east-1:777200079629:log-group:cloudtrail-logs:* \
     --cloud-watch-logs-role-arn arn:aws:iam::777200079629:role/Ab2dCloudTrailAssumeRole
   ```

##### Configure VPC flow log CloudWatch Log group for sandbox environment

1. Note that I am skipping this section since Splunk team already had a flow log created

   - /aws/lambda/client-logging-east-VPCFlowLogsFunction-1WH14LVH8S998

> *** TO DO ***: Verify this with Splunk team

1. Log on to the AWS sandbox account

1. Create a VPC flow log CloudWatch Log group

   1. Select **CloudWatch**

   1. Select **Log groups** from the leftmost panel

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log Group Name:** vpc-flowlogs

   1. Select **Create log group**

1. Create a VPC flow log

   1. Select **VPC**

   1. Select **Your VPCs** from the leftmost panel

   1. Select the following VPC

      ```
      ab2d-sbx-sandbox
      ```

   1. Select the **Flow Logs** tab

   1. Select **Create flow log**

   1. Configure the "Create flow log" page

      *Format:*

      - **Filter:** All

      - **Maximum aggregation interval:** 10 minutes

      - **Destination:** Send to CloudWatch Logs

      - **Destination log group:** vpc-flowlogs

      - **IAM role:** Ab2dInstanceRole

   1. Select **Create**

   1. Select **Close** on the "Create flow log" page

##### Onboard first deployment controller log to CloudWatch Log groups for sandbox environment

1. Connect to an API node in sandbox

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-sbx-sandbox
      ```

   1. Connect to the deployment controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

1. Download the CloudWatch Log agent

   1. Change to the "/tmp" directory

      ```ShellSession
      $ cd /tmp
      ```

   1. Download the CloudWatch Log Agent

      ```ShellSession
      $ curl -O https://s3.amazonaws.com/aws-cloudwatch/downloads/latest/awslogs-agent-setup.py
      ```

1. Configure the CloudWatch Log Agent and start logging "/var/log/messages"

   1. Enter the following

      ```ShellSession
      $ sudo python /tmp/awslogs-agent-setup.py --region us-east-1
      ```

   1. Wait for the following to display

      ```
      Step 1 of 5: Installing pip ...DONE
      ```

   1. Wait for the following to display

      *Note that this may take a while.*

      ```
      Step 2 of 5: Downloading the latest CloudWatch Logs agent bits ...DONE
      ```

   1. Note that the following is displayed

      ```
      Step 3 of 5: Configuring AWS CLI ...
      ```

   1. Press **return** on the keyboard to accept the default at the "AWS Access Key ID" prompt

   1. Press **return** on the keyboard to accept the default at the "AWS Secret Access Key" prompt

   1. Press **return** on the keyboard to accept the default at the "Default region name" prompt

   1. Press **return** on the keyboard to accept the default at the "Default output format" prompt

   1. Note that the following is displayed

      ```
      Step 4 of 5: Configuring the CloudWatch Logs Agent ...
      ```

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/messages

      - **Destination Log Group name:** /aws/ec2/var/log/messages

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** N

   1. Wait for the following to display

      ```
      Step 5 of 5: Setting up agent as a daemon ...DONE
      ```

   1. Note the following output

      ------------------------------------------------------
      - Configuration file successfully saved at: /var/awslogs/etc/awslogs.conf
      - You can begin accessing new log events after a few moments at https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logs:
      - You can use 'sudo service awslogs start|stop|status|restart' to control the daemon.
      - To see diagnostic information for the CloudWatch Logs Agent, see /var/log/awslogs.log
      - You can rerun interactive setup using 'sudo python ./awslogs-agent-setup.py --region us-east-1 --only-generate-config'
      ------------------------------------------------------

1. View the "/var/log/messages" entry in the newly created CloudWatch Log agent

   1. Enter the following
   
      ```ShellSession
      $ sudo cat /var/awslogs/etc/awslogs.conf | tail -7
      ```

   1. Note the output

      ```
      [/var/log/messages]
      datetime_format = %Y-%m-%d %H:%M:%S
      file = /var/log/messages
      buffer_duration = 5000
      log_stream_name = {instance_id}
      initial_position = start_of_file
      log_group_name = /aws/ec2/var/log/messages
      ```

1. Ensure the CloudWatch Log Agent is running

   1. Check the status of the CloudWatch Log Agent

      ```ShellSession
      $ service awslogs status
      ```

   1. If the CloudWatch Log Agent is not running, start it by entering the following

      ```ShellSession
      $ sudo service awslogs start
      ```

1. Exit the controler

   ```ShellSession
   $ exit
   ```

##### Onboard additional CloudWatch log groups for deployment controller log for sandbox environment

1. Connect to deployment controller

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      *Example for Mgmt environment:*

      ```ShellSession
      $ export AWS_PROFILE=ab2d-sbx-sandbox
      ```

   1. Connect to the sandbox controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

1. Onboard additional log groups

   1. Change to the "/tmp" directory

      ```ShellSession
      $ cd /tmp
      ```

   1. Run the interactive setup for the CloudWatch Log agent

      ```ShellSession
      $ sudo python ./awslogs-agent-setup.py \
        --region us-east-1 \
        --only-generate-config
      ```

   1. Note that the following is displayed

      ```
      Step 3 of 5: Configuring AWS CLI ...
      ```

   1. Press **return** on the keyboard to accept the default at the "AWS Access Key ID" prompt

   1. Press **return** on the keyboard to accept the default at the "AWS Secret Access Key" prompt

   1. Press **return** on the keyboard to accept the default at the "Default region name" prompt

   1. Press **return** on the keyboard to accept the default at the "Default output format" prompt

   1. Note that the following is displayed

      ```
      Step 4 of 5: Configuring the CloudWatch Logs Agent ...
      ```

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/amazon/ssm/amazon-ssm-agent.log

      - **Destination Log Group name:** /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/audit/audit.log

      - **Destination Log Group name:** /aws/ec2/var/log/audit/audit.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/awslogs.log

      - **Destination Log Group name:** /aws/ec2/var/log/awslogs.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cloud-init-output.log

      - **Destination Log Group name:** /aws/ec2/var/log/cloud-init-output.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cloud-init.log

      - **Destination Log Group name:** /aws/ec2/var/log/cloud-init.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cron

      - **Destination Log Group name:** /aws/ec2/var/log/cron

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/dmesg

      - **Destination Log Group name:** /aws/ec2/var/log/dmesg

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/maillog

      - **Destination Log Group name:** /aws/ec2/var/log/maillog

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/secure

      - **Destination Log Group name:** /aws/ec2/var/log/secure

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_agent-err.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_agent-err.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_agent.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_agent.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_am.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_am.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** N

   1. Note all the new CloudWatch log groups configurations are appended after the first "/var/log/messages" section in the configuaration file

      ```ShellSession
      $ sudo cat /var/awslogs/etc/awslogs.conf
      ```

1. Restart the awslogs service

   ```ShellSession
   $ sudo service awslogs restart
   ```

1. Reload systemd configuration

   ```ShellSession
   $ sudo systemctl daemon-reload
   ```

1. Exit deployment controller

   ```ShellSession
   $ exit
   ```

1. Verify that all expected CloudWatch Log groups are present

   1. Log on to the AWS sandbox environment account

   1. Select **CloudWatch**

   1. Select **Log groups** from the leftmost panel

   1. Verify that all of the following log groups are displayed
	
      - /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - /aws/ec2/var/log/audit/audit.log

      - /aws/ec2/var/log/awslogs.log

      - /aws/ec2/var/log/cloud-init-output.log

      - /aws/ec2/var/log/cloud-init.log

      - /aws/ec2/var/log/cron

      - /aws/ec2/var/log/dmesg

      - /aws/ec2/var/log/messages

      - /aws/ec2/var/log/secure

      - /aws/ec2/var/opt/ds_agent/diag/ds_agent-err.log

      - /aws/ec2/var/opt/ds_agent/diag/ds_agent.log

      - /aws/ec2/var/opt/ds_agent/diag/ds_am.log

      - vpc-flowlogs

      - cloudtrail-logs

1. If any of the log groups are missing, do the following:

   1. Note the missing group(s) so that you can later investigate why it is missing

      *Noted missing groups when creating on 2020-04-07:*

      - /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - /aws/ec2/var/log/cloud-init.log

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log group name:** {missing log group}

   1. Select **Create log group**

   1. Repeat this process for all missing groups

1. Select each log group and note which ones have no data

   *Noted log groups with no data when checking on 2020-04-07:*

   - /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

   - /aws/ec2/var/log/cloud-init.log

##### Onboard first api node log to CloudWatch Log groups for sandbox environment

> *** TO DO ***

1. Connect to an API node in sandbox

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      ```

   1. Connect to the sandbox controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Connect to the desired API node

      *Example for connecting to the first API node:*

      ```ShellSession
      $ ssh ec2-user@$(./list-api-instances.sh \
        | grep 10. \
        | awk '{print $2}' \
        | head -n 1)
      ```

> *** TO DO ***

##### Onboard additional CloudWatch log groups for first api node log for sandbox environment

> *** TO DO ***

1. Connect to an API node in sandbox

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      ```

   1. Connect to the sandbox controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Connect to the desired API node

      *Example for connecting to the first API node:*

      ```ShellSession
      $ ssh ec2-user@$(./list-api-instances.sh \
        | grep 10. \
        | awk '{print $2}' \
        | head -n 1)
      ```

> *** TO DO ***

#### Configure CloudWatch Log groups for impl environment

##### Configure CloudTrail CloudWatch Log group for impl environment

1. Log on to the AWS impl account

1. Create a CloudTrail CloudWatch Log group

   1. Select **Log groups** from the leftmost panel

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log Group Name:** cloudtrail-logs

   1. Select **Create log group**

1. Create a trail in CloudTrail

   1. Select **CloudTrail**

   1. Select **Create trail**

   1. Configure "Create Trail"

      - **Trail name:** cloudtrail-default

      - **Apply trail to all regions:** No

   1. Configure "Management events"

      - **Read/Write events:** All

      - **Log AWS KMS events:** Yes

   1. Configure "Insights events"

      - **Log Insights events:** No

   1. Configure "Data Events" for the "S3" tab

      - **Select all S3 buckets in your account:** Checked

   1. Configure "Storage location"

      - **Create a new S3 bucket:** No

      - **S3 bucket:** ab2d-sbx-impl-cloudtrail

   1. Select **Create**

1. Create a role for CloudTrail

   1. Set the AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-east-impl
      ```

   1. Change to the "Deploy" directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy
      ```

   1. Change to the shared directory

      ```ShellSession
      $ cd terraform/environments/ab2d-east-impl-shared
      ```

   1. Create the "Ab2dCloudTrailAssumeRole" role

      ```ShellSession
      $ aws --region us-east-1 iam create-role \
        --role-name Ab2dCloudTrailAssumeRole \
        --assume-role-policy-document file://ab2d-cloudtrail-assume-role-policy.json
      ```

   1. Add a CloudWatch log group policy to the CloudTrail role

      ```ShellSession
      $ aws --region us-east-1 iam put-role-policy \
        --role-name Ab2dCloudTrailAssumeRole \
	--policy-name Ab2dCloudTrailPolicy \
	--policy-document file://ab2d-cloudtrail-cloudwatch-policy.json
      ```

1. Update the trail in CloudTrail with the log group and role information

   ```ShellSession
   $ aws --region us-east-1 cloudtrail update-trail \
     --name cloudtrail-default \
     --cloud-watch-logs-log-group-arn arn:aws:logs:us-east-1:330810004472:log-group:cloudtrail-logs:* \
     --cloud-watch-logs-role-arn arn:aws:iam::330810004472:role/Ab2dCloudTrailAssumeRole
   ```

##### Configure VPC flow log CloudWatch Log group for impl environment

1. Log on to the AWS impl account

1. Create a VPC flow log CloudWatch Log group

   1. Select **CloudWatch**

   1. Select **Log groups** from the leftmost panel

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log Group Name:** vpc-flowlogs

   1. Select **Create log group**

1. Create a VPC flow log

   1. Select **VPC**

   1. Select **Your VPCs** from the leftmost panel

   1. Select the following VPC

      ```
      ab2d-east-impl
      ```

   1. Select the **Flow Logs** tab

   1. Select **Create flow log**

   1. Configure the "Create flow log" page

      *Format:*

      - **Filter:** All

      - **Maximum aggregation interval:** 10 minutes

      - **Destination:** Send to CloudWatch Logs

      - **Destination log group:** vpc-flowlogs

      - **IAM role:** Ab2dInstanceRole

   1. Select **Create**

   1. Select **Close** on the "Create flow log" page

##### Onboard first deployment controller log to CloudWatch Log groups for impl environment

1. Connect to an API node in impl

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-east-impl
      ```

   1. Connect to the deployment controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

1. Download the CloudWatch Log agent

   1. Change to the "/tmp" directory

      ```ShellSession
      $ cd /tmp
      ```

   1. Download the CloudWatch Log Agent

      ```ShellSession
      $ curl -O https://s3.amazonaws.com/aws-cloudwatch/downloads/latest/awslogs-agent-setup.py
      ```

1. Configure the CloudWatch Log Agent and start logging "/var/log/messages"

   1. Enter the following

      ```ShellSession
      $ sudo python /tmp/awslogs-agent-setup.py --region us-east-1
      ```

   1. Wait for the following to display

      ```
      Step 1 of 5: Installing pip ...DONE
      ```

   1. Wait for the following to display

      *Note that this may take a while.*

      ```
      Step 2 of 5: Downloading the latest CloudWatch Logs agent bits ...DONE
      ```

   1. Note that the following is displayed

      ```
      Step 3 of 5: Configuring AWS CLI ...
      ```

   1. Press **return** on the keyboard to accept the default at the "AWS Access Key ID" prompt

   1. Press **return** on the keyboard to accept the default at the "AWS Secret Access Key" prompt

   1. Press **return** on the keyboard to accept the default at the "Default region name" prompt

   1. Press **return** on the keyboard to accept the default at the "Default output format" prompt

   1. Note that the following is displayed

      ```
      Step 4 of 5: Configuring the CloudWatch Logs Agent ...
      ```

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/messages

      - **Destination Log Group name:** /aws/ec2/var/log/messages

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** N

   1. Wait for the following to display

      ```
      Step 5 of 5: Setting up agent as a daemon ...DONE
      ```

   1. Note the following output

      ------------------------------------------------------
      - Configuration file successfully saved at: /var/awslogs/etc/awslogs.conf
      - You can begin accessing new log events after a few moments at https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logs:
      - You can use 'sudo service awslogs start|stop|status|restart' to control the daemon.
      - To see diagnostic information for the CloudWatch Logs Agent, see /var/log/awslogs.log
      - You can rerun interactive setup using 'sudo python ./awslogs-agent-setup.py --region us-east-1 --only-generate-config'
      ------------------------------------------------------

1. View the "/var/log/messages" entry in the newly created CloudWatch Log agent

   1. Enter the following
   
      ```ShellSession
      $ sudo cat /var/awslogs/etc/awslogs.conf | tail -7
      ```

   1. Note the output

      ```
      [/var/log/messages]
      datetime_format = %Y-%m-%d %H:%M:%S
      file = /var/log/messages
      buffer_duration = 5000
      log_stream_name = {instance_id}
      initial_position = start_of_file
      log_group_name = /aws/ec2/var/log/messages
      ```

1. Ensure the CloudWatch Log Agent is running

   1. Check the status of the CloudWatch Log Agent

      ```ShellSession
      $ service awslogs status
      ```

   1. If the CloudWatch Log Agent is not running, start it by entering the following

      ```ShellSession
      $ sudo service awslogs start
      ```

1. Exit the controler

   ```ShellSession
   $ exit
   ```

##### Onboard additional CloudWatch log groups for deployment controller log for impl environment

1. Connect to deployment controller

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      *Example for Mgmt environment:*

      ```ShellSession
      $ export AWS_PROFILE=ab2d-east-impl
      ```

   1. Connect to the impl controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

1. Onboard additional log groups

   1. Change to the "/tmp" directory

      ```ShellSession
      $ cd /tmp
      ```

   1. Run the interactive setup for the CloudWatch Log agent

      ```ShellSession
      $ sudo python ./awslogs-agent-setup.py \
        --region us-east-1 \
        --only-generate-config
      ```

   1. Note that the following is displayed

      ```
      Step 3 of 5: Configuring AWS CLI ...
      ```

   1. Press **return** on the keyboard to accept the default at the "AWS Access Key ID" prompt

   1. Press **return** on the keyboard to accept the default at the "AWS Secret Access Key" prompt

   1. Press **return** on the keyboard to accept the default at the "Default region name" prompt

   1. Press **return** on the keyboard to accept the default at the "Default output format" prompt

   1. Note that the following is displayed

      ```
      Step 4 of 5: Configuring the CloudWatch Logs Agent ...
      ```

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/amazon/ssm/amazon-ssm-agent.log

      - **Destination Log Group name:** /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/audit/audit.log

      - **Destination Log Group name:** /aws/ec2/var/log/audit/audit.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/awslogs.log

      - **Destination Log Group name:** /aws/ec2/var/log/awslogs.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cloud-init-output.log

      - **Destination Log Group name:** /aws/ec2/var/log/cloud-init-output.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cloud-init.log

      - **Destination Log Group name:** /aws/ec2/var/log/cloud-init.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/cron

      - **Destination Log Group name:** /aws/ec2/var/log/cron

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/dmesg

      - **Destination Log Group name:** /aws/ec2/var/log/dmesg

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/maillog

      - **Destination Log Group name:** /aws/ec2/var/log/maillog

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/log/secure

      - **Destination Log Group name:** /aws/ec2/var/log/secure

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_agent-err.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_agent-err.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_agent.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_agent.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** Y

   1. Add the following log by entering the following at the prompts

      - **Path of log file to upload:** /var/opt/ds_agent/diag/ds_am.log

      - **Destination Log Group name:** /aws/ec2/var/opt/ds_agent/diag/ds_am.log

      - **Choose Log Stream Name:** 1 *Use EC2 instance id*

      - **Choose Log Event timestamp format:** 3 *%Y-%m-%d %H:%M:%S (2008-09-08 11:52:54)*

      - **Choose initial position of upload:** 1 *From start of file.*

      - **More log files to configure:** N

   1. Note all the new CloudWatch log groups configurations are appended after the first "/var/log/messages" section in the configuaration file

      ```ShellSession
      $ sudo cat /var/awslogs/etc/awslogs.conf
      ```

1. Restart the awslogs service

   ```ShellSession
   $ sudo service awslogs restart
   ```

1. Reload systemd configuration

   ```ShellSession
   $ sudo systemctl daemon-reload
   ```

1. Exit deployment controller

   ```ShellSession
   $ exit
   ```

1. Verify that all expected CloudWatch Log groups are present

   1. Log on to the AWS impl environment account

   1. Select **CloudWatch**

   1. Select **Log groups** from the leftmost panel

   1. Verify that all of the following log groups are displayed
	
      - /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - /aws/ec2/var/log/audit/audit.log

      - /aws/ec2/var/log/awslogs.log

      - /aws/ec2/var/log/cloud-init-output.log

      - /aws/ec2/var/log/cloud-init.log

      - /aws/ec2/var/log/cron

      - /aws/ec2/var/log/dmesg

      - /aws/ec2/var/log/messages

      - /aws/ec2/var/log/secure

      - /aws/ec2/var/opt/ds_agent/diag/ds_agent-err.log

      - /aws/ec2/var/opt/ds_agent/diag/ds_agent.log

      - /aws/ec2/var/opt/ds_agent/diag/ds_am.log

      - vpc-flowlogs

      - cloudtrail-logs

1. If any of the log groups are missing, do the following:

   1. Note the missing group(s) so that you can later investigate why it is missing

      *Noted missing groups when creating on 2020-04-07:*

      - /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

      - /aws/ec2/var/log/cloud-init.log

   1. Select **Actions**

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log group name:** {missing log group}

   1. Select **Create log group**

   1. Repeat this process for all missing groups

1. Select each log group and note which ones have no data

   *Noted log groups with no data when checking on 2020-04-07:*

   - /aws/ec2/var/log/amazon/ssm/amazon-ssm-agent.log

   - /aws/ec2/var/log/cloud-init.log

   - vpc-flowlogs

##### Onboard first api node log to CloudWatch Log groups for impl environment

> *** TO DO ***

1. Connect to an API node in impl

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-east-impl
      ```

   1. Connect to the impl controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Connect to the desired API node

      *Example for connecting to the first API node:*

      ```ShellSession
      $ ssh ec2-user@$(./list-api-instances.sh \
        | grep 10. \
        | awk '{print $2}' \
        | head -n 1)
      ```

> *** TO DO ***

##### Onboard additional CloudWatch log groups for first api node log for impl environment

> *** TO DO ***

1. Connect to an API node in impl

   1. Ensure that you are connected to the Cisco VPN

   1. Set the dev AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-dev
      ```

   1. Connect to the impl controller

      ```ShellSession
      $ ssh -i ~/.ssh/${AWS_PROFILE}.pem ec2-user@$(aws \
        --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Connect to the desired API node

      *Example for connecting to the first API node:*

      ```ShellSession
      $ ssh ec2-user@$(./list-api-instances.sh \
        | grep 10. \
        | awk '{print $2}' \
        | head -n 1)
      ```

> *** TO DO ***

## Appendix II: Get application load balancer access logs

1. Set AWS profile

   *Example for Sbx:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

1. Set the region

   *Example for Sbx:*

   ```ShellSession
   $ export REGION=us-east-1
   ```

1. Output a string that provides datetime and ip address of person or entity accessing the load balancer

   *Example for Sbx:*

   ```ShellSession
   $ aws s3api list-objects --bucket ab2d-sbx-sandbox-cloudtrail --query "Contents[*].Key"
   ```

## Appendix JJ: Export CloudWatch Log Group data to S3

1. Set the desired AWS profile

   *Set the Mgmt profile:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-mgmt-east-dev
   ```

1. Create an Amazon S3 Bucket

   ```ShellSession
   $ aws --region us-east-1 s3api create-bucket \
     --bucket ab2d-vpc-flow-log-backup-2020-04-03
   ```

1. Create a "cwl-export-user" IAM user

   ```ShellSession
   $ aws --region us-east-1 iam create-user \
     --user-name cwl-export-user
   ```

1. Get the IAM policy ARN for "AmazonS3FullAccess"

   ```ShellSession
   $ export S3_POLICY_ARN=$(aws --region us-east-1 iam list-policies \
     --query 'Policies[?PolicyName==`AmazonS3FullAccess`].{ARN:Arn}' \
     --output text)
   ```

1. Attach the "AmazonS3FullAccess" policy to the "cwl-export-user" IAM user

   ```ShellSession
   $ aws --region us-east-1 iam attach-user-policy \
    --user-name cwl-export-user \
    --policy-arn $S3_POLICY_ARN
   ```

1. Get the IAM policy ARN for "CloudWatchLogsFullAccess"

   ```ShellSession
   $ export CWL_POLICY_ARN=$(aws --region us-east-1 iam list-policies \
     --query 'Policies[?PolicyName==`CloudWatchLogsFullAccess`].{ARN:Arn}' \
     --output text)
   ```

1. Attach the "CloudWatchLogsFullAccess" policy to the "cwl-export-user" IAM user

   ```ShellSession
   $ aws --region us-east-1 iam attach-user-policy \
     --user-name cwl-export-user \
     --policy-arn $CWL_POLICY_ARN
   ```

1. Verify that the two managed policies are attached to the "cwl-export-user" user

   ```ShellSession
   $ aws --region us-east-1 iam list-attached-user-policies \
     --user-name cwl-export-user
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Change to the target environment directory

   ```ShellSession
   $ cd terraform/environments/ab2d-mgmt-east-dev
   ```

1. Set the bucket policy

   ```ShellSession
   $ aws --region us-east-1 s3api put-bucket-policy \
     --bucket ab2d-vpc-flow-log-backup-2020-04-03 \
     --policy file://ab2d-vpc-flow-log-backup-2020-04-03.json
   ```

1. Create an export task

   ```ShellSession
   $ aws --region us-east-1 logs create-export-task \
     --profile ab2d-mgmt-east-dev \
     --task-name "ab2d-vpc-flow-log-backup-2020-04-03" \
     --log-group-name "vpc-flow-log" \
     --from 1585789200000 \
     --to 1585962000000 \
     --destination "ab2d-vpc-flow-log-backup-2020-04-03" \
     --destination-prefix "export-task-output"
   ```

1. Note the output

   *Example:*

   ```
   {
     "taskId": "17ab8418-d69b-45b9-8c16-3fe77f339585"
   }
   ```

1. Check the status of the task

   *Format:*

   ```ShellSession
   $ aws --region us-east-1 logs describe-export-tasks \
     --profile ab2d-mgmt-east-dev  \
     --task-id "{task id}"
   ```

   *Example:*

   ```ShellSession
   $ aws --region us-east-1 logs describe-export-tasks \
     --profile ab2d-mgmt-east-dev  \
     --task-id "17ab8418-d69b-45b9-8c16-3fe77f339585"
   ```

1. Keep checking the status until the following is true

   ```
   "code": "COMPLETED",
   ```

## Appendix KK: Change the BFD certificate in AB2D keystores

1. Remove temporary directory (if exists)

   ```ShellSession
   $ rm -rf ~/Downloads/bfd-integration
   ```

1. Create a temporary directory

   ```ShellSession
   $ mkdir -p ~/Downloads/bfd-integration
   ```

1. Change to the temporary directory

   ```ShellSession
   $ cd ~/Downloads/bfd-integration
   ```

1. Download the following files from 1Password and copy them to "~/Downloads/bfd-integration"

   *Example for "Dev" environment:*
   
   - client_data_server_ab2d_dev_certificate.key

   - client_data_server_ab2d_dev_certificate.pem

   *Example for "Sbx" environment:*
   
   - client_data_server_ab2d_sbx_certificate.key

   - client_data_server_ab2d_sbx_certificate.pem

   *Example for "Impl" environment:*
   
   - client_data_server_ab2d_imp_certificate.key

   - client_data_server_ab2d_imp_certificate.pem

1. Create a keystore that includes the self-signed SSL certificate for AB2D client to BFD sandbox

   *Example for "Dev" environment:*

   ```ShellSession
   $ openssl pkcs12 -export \
     -in client_data_server_ab2d_dev_certificate.pem \
     -inkey client_data_server_ab2d_dev_certificate.key \
     -out ab2d_dev_keystore \
     -name client_data_server_ab2d_dev_certificate
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ openssl pkcs12 -export \
     -in client_data_server_ab2d_sbx_certificate.pem \
     -inkey client_data_server_ab2d_sbx_certificate.key \
     -out ab2d_sbx_keystore \
     -name client_data_server_ab2d_sbx_certificate
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ openssl pkcs12 -export \
     -in client_data_server_ab2d_imp_certificate.pem \
     -inkey client_data_server_ab2d_imp_certificate.key \
     -out ab2d_imp_keystore \
     -name client_data_server_ab2d_imp_certificate
   ```

1. Copy and paste the following password entry from 1Password at the "Enter Export Password" prompt

   *Example for "Dev" environment:*
   
   - **1Password entry:** AB2D Keystore for Dev: Password

   *Example for "Sbx" environment:*
   
   - **1Password entry:** AB2D Keystore for Sandbox: Password

   *Example for "Impl" environment:*
   
   - **1Password entry:** AB2D Keystore for Impl: Password
   
1. Note that the following file has been created

   *Example for "Dev" environment:*

   - ab2d_dev_keystore (keystore)

   *Example for "Sbx" environment:*

   - ab2d_sbx_keystore (keystore)

   *Example for "Impl" environment:*

   - ab2d_imp_keystore (keystore)

1. Send output from "prod-sbx.bfd.cms.gov" that includes only the certificate to a file

   ```ShellSession
   $ openssl s_client -connect prod-sbx.bfd.cms.gov:443 \
     2>/dev/null | openssl x509 -text \
     | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' \
     > prod-sbx.bfdcloud.pem
   ```

1. Note that the following file has been created

   - prod-sbx.bfdcloud.pem (certificate from the bfd sandbox server)

1. Import "prod-sbx.bfd.cms.gov" certificate into the keystore

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ keytool -import \
     -alias bfd-prod-sbx-selfsigned \
     -file prod-sbx.bfdcloud.pem \
     -storetype PKCS12 \
     -keystore ab2d_dev_keystore
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ keytool -import \
     -alias bfd-prod-sbx-selfsigned \
     -file prod-sbx.bfdcloud.pem \
     -storetype PKCS12 \
     -keystore ab2d_sbx_keystore
   ```

   *Example for "Impl" environment:*
   
   ```ShellSession
   $ keytool -import \
     -alias bfd-prod-sbx-selfsigned \
     -file prod-sbx.bfdcloud.pem \
     -storetype PKCS12 \
     -keystore ab2d_imp_keystore
   ```

1. Copy and paste the following password entry from 1Password at the "Enter keystore password" prompt

   *Example for "Dev" environment:*
   
   - **1Password entry:** AB2D Keystore for Dev: Password

   *Example for "Sbx" environment:*
   
   - **1Password entry:** AB2D Keystore for Sandbox: Password

   *Example for "Impl" environment:*
   
   - **1Password entry:** AB2D Keystore for Impl: Password

1. Enter the following at the "Trust this certificate" prompt

   ```
   yes
   ```

1. Verify that both the bfd sandbox and client certificates are present

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ keytool -list -v -keystore ab2d_dev_keystore
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ keytool -list -v -keystore ab2d_sbx_keystore
   ```

   *Example for "Impl" environment:*
   
   ```ShellSession
   $ keytool -list -v -keystore ab2d_imp_keystore
   ```

1. Copy and paste the following password entry from 1Password at the "Enter keystore password" prompt

   *Example for "Dev" environment:*
   
   - **1Password entry:** AB2D Keystore for Dev: Password

   *Example for "Sbx" environment:*
   
   - **1Password entry:** AB2D Keystore for Sandbox: Password

   *Example for "Impl" environment:*
   
   - **1Password entry:** AB2D Keystore for Impl: Password

1. Upload the keystore as a document in 1Password

   *Example for "Dev" environment:*
   
   - **1Password entry:** AB2D Keystore for Dev
   
   *Example for "Sbx" environment:*
   
   - **1Password entry:** AB2D Keystore for Sandbox

   *Example for "Impl" environment:*
   
   - **1Password entry:** AB2D Keystore for Impl

## Appendix LL: Update existing WAF

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set test parameters

   *Example for "Dev" environment:*

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev
   $ export REGION_PARAM=us-east-1
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export INTERNET_FACING_PARAM=false
   ```

1. Run application deployment automation

   ```ShellSession
   $ ./bash/update-waf.sh
   ```

## Appendix MM: Create new AMI from latest gold disk image

1. Ensure that your are connected to CMS Cisco VPN before proceeding

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set AWS environment variables using the CloudTamer API

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Enter the number of the desired AWS account where the desired logs reside

   *Example for Dev:*

   ```
   1 (Dev AWS account)
   ```

   *Example for Sbx:*

   ```
   2 (Sbx AWS account)
   ```

   *Example for Impl:*

   ```
   3 (Impl AWS account)
   ```

   *Example for Prod:*

   ```
   4 (Prod AWS account)
   ```

   *Example for Mgmt:*

   ```
   5 (Mgmt AWS account)
   ```

1. Note that temporary AWS credentials from CloudTamer will expire after an hour

1. Set gold disk test parameters

   *Example for "Dev":*

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export EC2_INSTANCE_TYPE_PACKER_PARAM=m5.xlarge
   $ export OWNER_PARAM=743302140042
   $ export REGION_PARAM=us-east-1
   $ export SSH_USERNAME_PARAM=ec2-user
   $ export VPC_ID_PARAM=vpc-0c6413ec40c5fdac3
   ```

   *Example for "Sbx":*

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-sbx-sandbox
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export EC2_INSTANCE_TYPE_PACKER_PARAM=m5.xlarge
   $ export OWNER_PARAM=743302140042
   $ export REGION_PARAM=us-east-1
   $ export SSH_USERNAME_PARAM=ec2-user
   $ export VPC_ID_PARAM=vpc-08dbf3fa96684151c
   ```

   *Example for "Prod":*

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-east-prod
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export EC2_INSTANCE_TYPE_PACKER_PARAM=m5.xlarge
   $ export OWNER_PARAM=743302140042
   $ export REGION_PARAM=us-east-1
   $ export SSH_USERNAME_PARAM=ec2-user
   $ export VPC_ID_PARAM=vpc-0c9d55c3d85f46a65
   ```

1. Run application deployment automation

   ```ShellSession
   $ ./bash/update-gold-disk.sh
   ```

## Appendix NN: Manually test the deployment

### Manually test the deployment for sandbox

1. Retrieve a JSON Web Token (JWT)

   1. Set the authorization for the test user

      *Example for test user 0oa2t0lsrdZw5uWRx297:*

      ```ShellSession
      $ AUTH=MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw==
      ```

   1. Retrieve a JWT bearer token by entering the following at the terminal prompt

      ```ShellSession
      $ BEARER_TOKEN=$(curl -X POST "https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -H "Accept: application/json" \
        -H "Authorization: Basic ${AUTH}" \
        | jq --raw-output ".access_token")
      ```

   1. Ensure that you have a "BEARER_TOKEN" environment variable before proceeding

      ```ShellSession
      $ echo $BEARER_TOKEN
      ```

1. Create an export job

   *Example for "Dev" environment:*

   *Temporarily using "--insecure" parameter until onboarded for DNS routing:*
   
   ```ShellSession
   $ curl "https://internal-ab2d-dev-820359992.us-east-1.elb.amazonaws.com/api/v1/fhir/Patient/\$export?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit" \
     -sD - \
     --insecure \
     -H "Accept: application/json" \
     -H "Prefer: respond-async" \
     -H "Authorization: Bearer ${BEARER_TOKEN}"

   $ curl "https://dev.ab2d.cms.gov/api/v1/fhir/Patient/\$export?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit" \
     -sD - \
     --insecure \
     -H "Accept: application/json" \
     -H "Prefer: respond-async" \
     -H "Authorization: Bearer ${BEARER_TOKEN}"
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ curl "https://sandbox.ab2d.cms.gov/api/v1/fhir/Patient/\$export?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit" \
     -sD - \
     -H "Accept: application/json" \
     -H "Prefer: respond-async" \
     -H "Authorization: Bearer ${BEARER_TOKEN}"
   ```

1. Note the output

   *Format:*

   ```
   HTTP/2 {response code}
   Date: Mon, 13 Apr 2020 16:35:38 GMT
   content-length: 0
   vary: Origin
   vary: Access-Control-Request-Method
   vary: Access-Control-Request-Headers
   vary: Origin
   vary: Access-Control-Request-Method
   vary: Access-Control-Request-Headers
   content-location: https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/{job id}/$status
   x-content-type-options: nosniff
   x-xss-protection: 1; mode=block
   cache-control: no-cache, no-store, max-age=0, must-revalidate
   pragma: no-cache
   expires: 0
   x-frame-options: DENY
   ```

   *Example for "Dev" environment:*

   ```
   HTTP/1.1 202
   Date: Mon, 13 Apr 2020 16:35:38 GMT
   Content-Length: 0
   Connection: keep-alive
   Vary: Origin
   Vary: Access-Control-Request-Method
   Vary: Access-Control-Request-Headers
   Vary: Origin
   Vary: Access-Control-Request-Method
   Vary: Access-Control-Request-Headers
   Content-Location: http://internal-ab2d-dev-820359992.us-east-1.elb.amazonaws.com/api/v1/fhir/Job/42d7addc-0e1b-4687-a1e2-5e029f173849/$status
   X-Content-Type-Options: nosniff
   X-XSS-Protection: 1; mode=block
   Cache-Control: no-cache, no-store, max-age=0, must-revalidate
   Pragma: no-cache
   Expires: 0
   X-Frame-Options: DENY
   ```

   *Example for "Sbx" environment:*

   ```
   HTTP/2 202
   Date: Mon, 13 Apr 2020 16:35:38 GMT
   content-length: 0
   vary: Origin
   vary: Access-Control-Request-Method
   vary: Access-Control-Request-Headers
   vary: Origin
   vary: Access-Control-Request-Method
   vary: Access-Control-Request-Headers
   content-location: https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/09b4d5e6-92f7-4dcb-8b49-dbdd2e22dc69/$status
   x-content-type-options: nosniff
   x-xss-protection: 1; mode=block
   cache-control: no-cache, no-store, max-age=0, must-revalidate
   pragma: no-cache
   expires: 0
   x-frame-options: DENY
   ```

1. Note the response code and job id from the output

   ```
   {response-code} = 202 
   {job id} = 42d7addc-0e1b-4687-a1e2-5e029f173849
   ```

1. If the response code is 202, set an environment variable for the job by entering the following at the terminal prompt

   *Format:*

   ```ShellSession
   $ JOB={job id}
   ```
   
   *Example:*

   ```ShellSession
   $ JOB=42d7addc-0e1b-4687-a1e2-5e029f173849
   ```

1. Check the status of the job by entering the following at the terminal prompt
   
   ```ShellSession
   $ curl "https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/${JOB}/\$status" \
     -sD - \
     -H "accept: application/json" \
     -H "Authorization: Bearer ${BEARER_TOKEN}"
   ```

1. Note the output

   *Format:*

   ```
   HTTP/1.1 {response-code} 
   Date: Mon, 13 Apr 2020 22:13:32 GMT
   Content-Type: application/json
   Transfer-Encoding: chunked
   Connection: keep-alive
   Vary: accept-encoding,origin,access-control-request-headers,access-control-request-method,accept-encoding
   X-Frame-Options: DENY
   X-XSS-Protection: 1; mode=block
   X-Content-Type-Options: nosniff
   Expires: Tue, 14 Apr 2020 22:11:39 GMT
   
   {"transactionTime":"Apr 13, 2020, 10:11:35 PM","request":"http://internal-ab2d-dev-820359992.us-east-1.elb.amazonaws.com/api/v1/fhir/Patient/$export?_outputFormat=application%252Ffhir%252Bndjson&_type=ExplanationOfBenefit","requiresAccessToken":true,"output":[{"type":"ExplanationOfBenefit","url":"http://internal-ab2d-dev-820359992.us-east-1.elb.amazonaws.com/api/v1/fhir/Job/42d7addc-0e1b-4687-a1e2-5e029f173849/file/{file to download}","extension":[{"url":"https://ab2d.cms.gov/checksum","valueString":"sha256:46ccda6384b31693c27d057500a4ee116cd6f0540b3370a7e4d50c649ea8da27"},{"url":"https://ab2d.cms.gov/file_length","valueDecimal":9194196}]}],"error":[]}
   ```

   *Example:*

   ```
   HTTP/1.1 200 
   Date: Mon, 13 Apr 2020 22:13:32 GMT
   Content-Type: application/json
   Transfer-Encoding: chunked
   Connection: keep-alive
   Vary: accept-encoding,origin,access-control-request-headers,access-control-request-method,accept-encoding
   X-Frame-Options: DENY
   X-XSS-Protection: 1; mode=block
   X-Content-Type-Options: nosniff
   Expires: Tue, 14 Apr 2020 22:11:39 GMT
   
   {"transactionTime":"Apr 13, 2020, 10:11:35 PM","request":"http://internal-ab2d-dev-820359992.us-east-1.elb.amazonaws.com/api/v1/fhir/Patient/$export?_outputFormat=application%252Ffhir%252Bndjson&_type=ExplanationOfBenefit","requiresAccessToken":true,"output":[{"type":"ExplanationOfBenefit","url":"http://internal-ab2d-dev-820359992.us-east-1.elb.amazonaws.com/api/v1/fhir/Job/42d7addc-0e1b-4687-a1e2-5e029f173849/file/S0000_0001.ndjson","extension":[{"url":"https://ab2d.cms.gov/checksum","valueString":"sha256:46ccda6384b31693c27d057500a4ee116cd6f0540b3370a7e4d50c649ea8da27"},{"url":"https://ab2d.cms.gov/file_length","valueDecimal":9194196}]}],"error":[]}   
   ```

1. If the status is 202, do the following

   1. Note the following in the output, for example:

      *Format:*
      
      ```
      x-progress: {percentage} complete
      ```

      *Example:*
      
      ```
      x-progress: 7% complete
      ```

   1. Based on the progress, you can a wait a period of time and try the status check again until you see a status of 200

1. If the status is 200, download the files by doing the following:

   1. Set an environment variable to the first file to download

      ```ShellSession
      $ FILE=Z0000_0001.ndjson
      ```

   1. Get the Part A & B bulk claim export data by entering the following at the terminal prompt

      ```ShellSession
      $ curl "https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/${JOB}/file/${FILE}" \
        -H "Authorization: Bearer ${BEARER_TOKEN}" \
        > ${FILE}
      ```

1. View the downloaded file

   ```ShellSession
   $ cat $FILE
   ```

1. Try the download operation a second time

   ```ShellSession
   $ curl "https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/${JOB}/file/${FILE}" \
     -sD - \
     -H "Accept: application/fhir+ndjson" \
     -H "Authorization: Bearer ${BEARER_TOKEN}"
   ```

1. Verify that the following error message is displayed

   ```
   {"resourceType":"OperationOutcome","issue":[{"severity":"error","code":"invalid","details":{"text":"The file is not present as it has already been downloaded. Please resubmit the job."}}]}
   ```

### Manually test the deployment for production

1. Retrieve a JSON Web Token (JWT)

   1. Set "AB2D Prod : OKTA Prod : AB2D - PDP-1000 : Client ID" from 1Password

      ```ShellSession
      $ OKTA_CLIENT_ID={okta ab2d admin client id}
      ```

   1. Set "AB2D Prod : OKTA Prod : AB2D - PDP-1000 : Client Secret" from 1Password

      ```ShellSession
      $ OKTA_CLIENT_PASSWORD={okta ab2d admin client secret}
      ```

   1. Set the authorization for the test user

      ```ShellSession
      $ AUTH=$(echo -n "${OKTA_CLIENT_ID}:${OKTA_CLIENT_PASSWORD}" | base64)
      ```

   1. Retrieve a JWT bearer token by entering the following at the terminal prompt

      ```ShellSession
      $ BEARER_TOKEN=$(curl -X POST "https://idm.cms.gov/oauth2/aus2ytanytjdaF9cr297/v1/token?grant_type=client_credentials&scope=clientCreds" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -H "Accept: application/json" \
        -H "Authorization: Basic ${AUTH}" \
        | jq --raw-output ".access_token")
      ```

   1. Ensure that you have a "BEARER_TOKEN" environment variable before proceeding

      ```ShellSession
      $ echo $BEARER_TOKEN
      ```

1. Create an export job

   ```ShellSession
   $ curl "https://api.ab2d.cms.gov/api/v1/fhir/Patient/\$export?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit" \
     -sD - \
     -H "Accept: application/json" \
     -H "Prefer: respond-async" \
     -H "Authorization: Bearer ${BEARER_TOKEN}"
   ```

1. Note the output

   *Format:*

   ```
   HTTP/2 {response code} 
   content-length: 0
   content-location: https://api.ab2d.cms.gov/api/v1/fhir/Job/{job id}/$status
   x-xss-protection: 1; mode=block
   x-frame-options: DENY
   expires: Mon, 29 Jun 2020 20:16:25 GMT
   date: Mon, 29 Jun 2020 20:16:25 GMT
   strict-transport-security: max-age=86400
   x-content-type-options: nosniff
   pragma: no-cache
   cache-control: max-age=0, 
   ```

   *Example:*

   ```
   HTTP/2 202 
   content-length: 0
   content-location: https://api.ab2d.cms.gov/api/v1/fhir/Job/a4ab9339-5865-4a28-823e-8aa0a18b68b3/$status
   x-xss-protection: 1; mode=block
   x-frame-options: DENY
   expires: Mon, 29 Jun 2020 20:16:25 GMT
   date: Mon, 29 Jun 2020 20:16:25 GMT
   strict-transport-security: max-age=86400
   x-content-type-options: nosniff
   pragma: no-cache
   cache-control: max-age=0, 
   ```

1. Note the response code and job id from the output

   ```
   {response-code} = 202 
   {job id} = 31624a77-6515-4e59-aabe-5e8a192d2d7f
   ```

1. If the response code is 202, set an environment variable for the job by entering the following at the terminal prompt

   *Format:*

   ```ShellSession
   $ JOB={job id}
   ```
   
   *Example:*

   ```ShellSession
   $ JOB=31624a77-6515-4e59-aabe-5e8a192d2d7f
   ```

1. Check the status of the job by entering the following at the terminal prompt
   
   ```ShellSession
   $ curl "https://api.ab2d.cms.gov/api/v1/fhir/Job/${JOB}/\$status" \
     -sD - \
     -H "accept: application/json" \
     -H "Authorization: Bearer ${BEARER_TOKEN}"
   ```

1. Note the output

   *Format:*

   ```
   HTTP/2 {response-code}
   content-type: application/json
   x-xss-protection: 1; mode=block
   x-frame-options: DENY
   expires: Mon, 29 Jun 2020 21:09:59 GMT
   date: Mon, 29 Jun 2020 21:09:59 GMT
   content-length: 583
   strict-transport-security: max-age=86400
   x-content-type-options: nosniff
   pragma: no-cache
   cache-control: max-age=0, no-cache, no-store, must-revalidate
   x-akamai-staging: ESSL

   {"transactionTime":"Jun 29, 2020, 8:16:25 PM","request":"https://api.ab2d.cms.gov/api/v1/fhir/Patient/$export?_outputFormat=application%252Ffhir%252Bndjson&_type=ExplanationOfBenefit","requiresAccessToken":true,"output":[{"type":"ExplanationOfBenefit","url":"https://api.ab2d.cms.gov/api/v1/fhir/Job/{job id}/file/{file to download}","extension":[{"url":"https://ab2d.cms.gov/checksum","valueString":"sha256:d5f1b0d39f5310707e21e83861e121d416df1ce1d6a6e1fd497cb6bde2ea32d3"},{"url":"https://ab2d.cms.gov/file_length","valueDecimal":30841503}]}],"error":[]}
   ```

   *Example:*

   ```
   HTTP/2 200 
   content-type: application/json
   x-xss-protection: 1; mode=block
   x-frame-options: DENY
   expires: Mon, 29 Jun 2020 21:09:59 GMT
   date: Mon, 29 Jun 2020 21:09:59 GMT
   content-length: 583
   strict-transport-security: max-age=86400
   x-content-type-options: nosniff
   pragma: no-cache
   cache-control: max-age=0, no-cache, no-store, must-revalidate
   x-akamai-staging: ESSL

   {"transactionTime":"Jun 29, 2020, 8:16:25 PM","request":"https://api.ab2d.cms.gov/api/v1/fhir/Patient/$export?_outputFormat=application%252Ffhir%252Bndjson&_type=ExplanationOfBenefit","requiresAccessToken":true,"output":[{"type":"ExplanationOfBenefit","url":"https://api.ab2d.cms.gov/api/v1/fhir/Job/a4ab9339-5865-4a28-823e-8aa0a18b68b3/file/S8067_0001.ndjson","extension":[{"url":"https://ab2d.cms.gov/checksum","valueString":"sha256:d5f1b0d39f5310707e21e83861e121d416df1ce1d6a6e1fd497cb6bde2ea32d3"},{"url":"https://ab2d.cms.gov/file_length","valueDecimal":30841503}]}],"error":[]}
   ```

1. If the status is 202, do the following

   1. Note the following in the output, for example:

      *Format:*
      
      ```
      x-progress: {percentage} complete
      ```

      *Example:*
      
      ```
      x-progress: 7% complete
      ```

   1. Based on the progress, you can a wait a period of time and try the status check again until you see a status of 200

1. If the status is 200, download the files by doing the following:

   1. Set an environment variable to the first file to download

      *Example:*

      ```ShellSession
      $ FILE=S8067_0001.ndjson
      ```

   1. Get the Part A & B bulk claim export data by entering the following at the terminal prompt

      ```ShellSession
      $ curl "https://api.ab2d.cms.gov/api/v1/fhir/Job/${JOB}/file/${FILE}" \
        -H "Accept: application/fhir+ndjson" \
        -H "Authorization: Bearer ${BEARER_TOKEN}" \
        > ${FILE}
      ```

1. View the downloaded file

   ```ShellSession
   $ cat $FILE
   ```

1. Try the download operation a second time

   ```ShellSession
   $ curl "https://api.ab2d.cms.gov/api/v1/fhir/Job/${JOB}/file/${FILE}" \
     -H "Accept: application/fhir+ndjson" \
     -H "Authorization: Bearer ${BEARER_TOKEN}"
   ```

1. Verify that the following error message is displayed

   ```
   {"resourceType":"OperationOutcome","issue":[{"severity":"error","code":"invalid","details":{"text":"The file is not present as it has already been downloaded. Please resubmit the job."}}]}
   ```

## Appendix OO: Merge a specific commit from master into your branch

1. Ensure that you have committed or stashed any changes in your current branch

1. Update "origin/master"

   ```ShellSession
   $ git fetch --all
   ```

1. Change to the master branch

   ```ShellSession
   $ git checkout master
   ```

1. Update master with the latest from GitHub

   ```ShellSession
   $ git pull
   ```

1. Create a temporary release branch

   *Format:*
   
   ```ShellSession
   $ git checkout -b temporary-release-branch {desired commit number}
   ```

1. Checkout your deployment branch

   *Format:*
   
   ```ShellSession
   $ git checkout {your deployment branch}
   ```

1. Merge the temporary release branch into your deployment branch

   ```ShellSession
   $ git merge temporary-release-branch
   ```

1. Handle any conflicts, commit, and push

1. Delete the temorary release branch

   ```ShellSession
   $ git branch -D temporary-release-branch
   ```

## Appendix PP: Test running development automation from development machine

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set test parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev \
     && export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev \
     && export REGION_PARAM=us-east-1 \
     && export VPC_ID_PARAM=vpc-0c6413ec40c5fdac3 \
     && export SSH_USERNAME_PARAM=ec2-user \
     && export EC2_INSTANCE_TYPE_API_PARAM=m5.xlarge \
     && export EC2_INSTANCE_TYPE_WORKER_PARAM=m5.xlarge \
     && export EC2_DESIRED_INSTANCE_COUNT_API_PARAM=1 \
     && export EC2_MINIMUM_INSTANCE_COUNT_API_PARAM=1 \
     && export EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM=1 \
     && export EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM=1 \
     && export EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM=1 \
     && export EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM=1 \
     && export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export INTERNET_FACING_PARAM=false \
     && export CLOUD_TAMER_PARAM=true
   ```

1. Run application deployment automation

   ```ShellSession
   $ ./bash/deploy-application.sh
   ```

## Appendix QQ: Set up demonstration of cross account access of an encrypted S3 bucket

1. Open a new terminal

1. Set AWS credentials to the SemanticBits demo account

   ```ShellSession
   $ export AWS_PROFILE=sbdemo-shared
   ```

1. Set default AWS region

   ```ShellSession
   $ export AWS_DEFAULT_REGION=us-east-1
   ```
   
1. Set test bucket name

   *Example:*
   
   ```ShellSession
   $ S3_TEST_BUCKET_NAME="ab2d-optout"
   ```

1. Determine if bucket already exists

   ```ShellSession
   $ S3_TEST_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
     --query "Buckets[?Name=='${S3_TEST_BUCKET_NAME}'].Name" \
     --output text)
   ```

1. Create the bucket (if doesn't exist)

   ```ShellSession
   $ if [ -z "${S3_AUTOMATION_BUCKET_EXISTS}" ]; then \
     aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
       --bucket ${S3_TEST_BUCKET_NAME}; \
     fi
   ```

1. Block public access on the bucket

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket ${S3_TEST_BUCKET_NAME} \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

1. Set kms key name

   ```ShellSession
   $ S3_TEST_BUCKET_KEY_NAME="${S3_TEST_BUCKET_NAME}-key"
   ```
   
1. Create a kms key for the bucket

   ```ShellSession
   $ S3_TEST_BUCKET_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms create-key \
     | jq --raw-output ".KeyMetadata.KeyId")
   ```

1. Create an alias for the kms key for the bucket

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" kms create-alias \
     --alias-name "alias/${S3_TEST_BUCKET_NAME}" \
     --target-key-id "${S3_TEST_BUCKET_KEY_ID}"
   ```

> *** TO DO ***

## Appendix RR: Tealium and Google Analytics notes

*Here is the service desk ticket that we created:*

> https://jira.cms.gov/browse/WHSD-24539

*Here is documentation that CMS support created that let us know what to add to the website pages:*

> https://confluence.cms.gov/display/BLSTANALYT/Tealium+Implementation+Documentation#TealiumImplementationDocumentation-ab2d.cms.gov

*This is part we added for our development version of our Jekyll static website:*

> https://github.com/CMSgov/ab2d/blob/master/website/_includes/head.html#L26-L49

*For the production static website, I just changed the "head.html" from dev to prod like this:*

```ShellSession
$ sed -i "" 's%cms-ab2d[\/]prod%cms-ab2d/dev%g' _includes/head.html
```

## Appendix SS: Destroy application

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set the target environment

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Select the target environment by entering the corresponding number on the keyboard

1. Change to the target environment

   ```ShellSession
   $ cd "terraform/environments/${CMS_ENV}"
   ```

1. Destroy WAF

   ```ShellSession
   $ terraform destroy \
     --target module.waf \
     --auto-approve
   ```

1. Destroy CloudWatch

   ```ShellSession
   $ terraform destroy \
     --target module.cloudwatch \
     --auto-approve
   ```

1. Destroy Worker

   ```ShellSession
   $ terraform destroy \
     --target module.worker \
     --auto-approve
   ```

1. Take delete protection off the load balancer

   > *** TO DO ***
   
1. Destroy API

   ```ShellSession
   $ terraform destroy \
     --target module.api \
     --auto-approve
   ```

## Appendix TT: Migrate terraform state from shared environment to main environment

1. Ensure that you are connected to CMS Cisco VPN

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set AWS environment variables using the CloudTamer API

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Choose desired environment

   *Example for "Dev" environment:*

   ```
   1 (Dev AWS account)
   ```

1. Change to target directory

   *Example for "Dev" environment:*

   ```ShellSession
   $ cd terraform/environments/ab2d-dev
   ```

1. List and note the current modules under the target environment

   ```ShellSession
   $ terraform state list
   ```

1. Pull the terraform state file for the target environment from S3

   ```ShellSession
   $ terraform state pull > terraform.tfstate
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Change to source directory

   *Example for "Dev" environment:*

   ```ShellSession
   $ cd terraform/environments/ab2d-dev-shared
   ```

1. List and note the current modules under the source environment

   ```ShellSession
   $ terraform state list
   ```

1. Move the state of a module from the source environment to the target environment state file

   *Example #1:*
   
   ```ShellSession
   $ terraform state mv \
     -state-out=../ab2d-dev/terraform.tfstate \
     null_resource.authorized_keys_file \
     null_resource.authorized_keys_file
   ```

   *Example #2:*
   
   ```ShellSession
   $ terraform state mv \
     -state-out=../ab2d-dev/terraform.tfstate \
     module.controller.aws_eip.deployment_controller \
     module.controller.aws_eip.deployment_controller
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Change to target directory

   *Example for "Dev" environment:*

   ```ShellSession
   $ cd terraform/environments/ab2d-dev
   ```

1. Push the modified terraform state file to S3

   ```ShellSession
   $ terraform state push terraform.tfstate
   ```

1. Delete the ".terraform" directory

   ```ShellSession
   $ rm -rf .terraform
   ```
   
1. Backup the local terraform state file and its backup files

   1. Create a backup directory
   
      ```ShellSession
      $ mkdir -p ~/Downloads/backup
      ```

   1. Move local terraform state file and its backup files to backup directory

      ```ShellSession
      $ mv terraform.tfstate* ~/Downloads/backup
      ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Deploy infrastructure

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ ./deploy-infrastructure.sh \
     --environment=ab2d-dev \
     --ecr-repo-environment=ab2d-mgmt-east-dev \
     --region=us-east-1 \
     --vpc-id=vpc-0c6413ec40c5fdac3 \
     --ssh-username=ec2-user \
     --owner=743302140042 \
     --ec2_instance_type_api=m5.xlarge \
     --ec2_instance_type_worker=m5.xlarge \
     --ec2_instance_type_other=m5.xlarge \
     --ec2_desired_instance_count_api=1 \
     --ec2_minimum_instance_count_api=1 \
     --ec2_maximum_instance_count_api=1 \
     --ec2_desired_instance_count_worker=1 \
     --ec2_minimum_instance_count_worker=1 \
     --ec2_maximum_instance_count_worker=1 \
     --database-secret-datetime=2020-01-02-09-15-01 \
     --build-new-images \
     --internet-facing=false \
     --auto-approve
   ```

1. Set parameters

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev
   $ export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev
   $ export REGION_PARAM=us-east-1
   $ export VPC_ID_PARAM=vpc-0c6413ec40c5fdac3
   $ export SSH_USERNAME_PARAM=ec2-user
   $ export EC2_INSTANCE_TYPE_API_PARAM=m5.xlarge
   $ export EC2_INSTANCE_TYPE_WORKER_PARAM=m5.xlarge
   $ export EC2_DESIRED_INSTANCE_COUNT_API_PARAM=1
   $ export EC2_MINIMUM_INSTANCE_COUNT_API_PARAM=1
   $ export EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM=1
   $ export EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM=1
   $ export EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM=1
   $ export EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM=1
   $ export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export INTERNET_FACING_PARAM=false
   $ export CLOUD_TAMER_PARAM=true
   ``` 

1. Deploy application

   ```ShellSession
   $ ./bash/deploy-application.sh
   ```
   
> *** TO DO ***: Complete moving modules from ab2d-sbx-sandbox-shared to ab2d-sbx-sandbox

> *** TO DO ***: Complete moving modules from ab2d-east-impl-shared to ab2d-east-impl

> *** TO DO ***: Eliminate shared environments from branch

## Appendix UU: Access Health Plan Management System (HPMS)

1. Request aprroval for the following EUA job code

   ```
   HPMS_Prod_AWS
   ```

1. Wait to be approved for the "HPMS_Prod_AWS" job code

1. Open Chrome

1. Enter the following in the address bar

   > https://hpms.cms.gov

1. Log on with your standard EUA credentials

1. Scroll down to the bottom of the page

1. Select the **Accept** radio button

1. Select **Submit**

1. Complete required fields on the "User Account Management" page

1. Select **Save**

## Appendix VV: Import an existing resource using terraform

### Import an existing IAM role

1. Get credentials to the target AWS environment

   1. Enter the following

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Respond to the prompts to change to the desired AWS environment

1. Change to the target terraform environment

   *Example for "Impl" environment:*

    ```ShellSession
    $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl
    ```

1. Import an existing IAM role that is not currently managed by terraform

   *Note that the IAM role must already be defined in terraform.*

   *Example of importing existing "Ab2dInstanceRole" IAM role:"

   ```ShellSession
   $ terraform import module.iam.aws_iam_role.ab2d_instance_role Ab2dInstanceRole
   ```

1. Verify that the import was successful based on expected output

   ```
   module.iam.aws_iam_role.ab2d_instance_role: Importing from ID "Ab2dInstanceRole"...
   module.iam.aws_iam_role.ab2d_instance_role: Import prepared!
     Prepared aws_iam_role for import
   module.iam.aws_iam_role.ab2d_instance_role: Refreshing state... [id=Ab2dInstanceRole]

   Import successful!

   The resources that were imported are shown above. These resources are now in
   your Terraform state and will henceforth be managed by Terraform.
   ```

### Import an existing IAM Instance Profile

1. Get credentials to the target AWS environment

   1. Enter the following

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Respond to the prompts to change to the desired AWS environment

1. Change to the target terraform environment

   *Example for "Impl" environment:*

    ```ShellSession
    $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl
    ```

1. Import an existing IAM instance profile that is not currently managed by terraform

   *Note that the IAM instance profile must already be defined in terraform.*

   *Example of importing existing "Ab2dInstanceProfile" IAM instance profile:"

   ```ShellSession
   $ terraform import module.iam.aws_iam_instance_profile.test_profile Ab2dInstanceProfile
   ```

1. Verify that the import was successful based on expected output

   ```
   module.iam.aws_iam_instance_profile.test_profile: Import prepared!
     Prepared aws_iam_instance_profile for import
   module.iam.aws_iam_instance_profile.test_profile: Refreshing state... [id=Ab2dInstanceProfile]
   
   Import successful!
   
   The resources that were imported are shown above. These resources are now in
   your Terraform state and will henceforth be managed by Terraform.
   ```

### Import an existing KMS key

1. Get credentials to the target AWS environment

   1. Enter the following

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Respond to the prompts to change to the desired AWS environment

1. Change to the target terraform environment

   *Example for "Impl" environment:*

    ```ShellSession
    $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl
    ```

1. Get the KMS key id

   ```ShellSession
   $ KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
     --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
     --output text)
   ```

1. Import an existing KMS key that is not currently managed by terraform

   *Note that the KMS key must already be defined in terraform.*

   *Example of importing existing "Ab2dInstanceProfile" IAM instance profile:"

   ```ShellSession
   $ terraform import module.kms.aws_kms_key.a "${KMS_KEY_ID}"
   ```

1. Verify that the import was successful

## Appendix WW: Use an SSH tunnel to query production database from local machine

1. Ensure that you are connected to VPN

1. Download "AB2D Prod - EC2 Instances - Private Key" from 1Password

   ```
   ab2d-east-prod.pem
   ```
   
1. Save the key to the "~/.ssh" directory

1. Change the permissions on the key

   ```ShellSession
   $ chmod 600 ~/.ssh/ab2d-east-prod.pem
   ```

1. Change to your "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set the production environment

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Choose a local port that you want to use for the SSH tunnel and set an environment variable

   *Example:*

   ```ShellSession
   $ LOCAL_DB_PORT=1234
   ```

1. Set controller private IP address

   ```ShellSession
   $ CONTROLLER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text)
   ```

1. Get database host

   ```ShellSession
   $ DATABASE_SECRET_DATETIME="2020-01-02-09-15-01" \
     && DATABASE_HOST=$(./Deploy/python3/get-database-secret.py $CMS_ENV database_host $DATABASE_SECRET_DATETIME)
   ```

1. Start the SSH tunnel to the production database

   ```ShellSession
   $ ssh -N -L \
     "${LOCAL_DB_PORT}:${DATABASE_HOST}:5432" \
     ec2-user@"${CONTROLLER_PRIVATE_IP}" \
     -i "~/.ssh/${SSH_PRIVATE_KEY}"
   ```

1. Note that the terminal tab will not return to the prompt while the tunnel is running, so don't close the terminal tab while using the tunnel

1. Connect to the production database using your desired method (I tested it with pgAdmin)

   - **host:** 127.0.0.1
   
   - **port:** 1234
   
   - **username:** {database username}
   
   - **password:** {database password}
   
1. Note the following:

   - if you don't maintain your connection to the database, the EC2 instance that you are tunneling through will auto-logout (this is due to the inactivity timeout set on the gold disks)

   - if your tunnel closes, you will need to rerun the SSH tunnel command

## Appendix XX: Create a self-signed certificate for an EC2 load balancer

1. Remove existing directory (if exists)

   ```ShellSession
   $ rm -rf ~/Downloads/ab2dtemp
   ```

1. Create the working directory

   ```ShellSession
   $ mkdir -p ~/Downloads/ab2dtemp
   ```

1. Change to the working directory

   ```ShellSession
   $ cd ~/Downloads/ab2dtemp
   ```

1. Set common name

   ```ShellSession
   $ export COMMON_NAME="ab2dtemp_com"
   ```

1. Set certificate name

   ```ShellSession
   $ export CERTIFICATE_NAME="Ab2dTempCom"
   ```

1. Create private key and self-sig

   ```ShellSession
   $ openssl req \
     -nodes -x509 \
     -days 1825 \
     -newkey rsa:4096 \
     -keyout "${COMMON_NAME}.key" \
     -subj "/CN=${COMMON_NAME}" \
     -out "${COMMON_NAME}_certificate.pem"
   ```

1. Note the following files were created

   - ab2dtemp_com.key

   - ab2dtemp_com_certificate.pem

1. Set target environment

   ```ShellSession
   $ source ~/code/ab2d/Deploy/bash/set-env.sh
   ```

1. Create an IAM server certificate

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" iam upload-server-certificate \
     --server-certificate-name "${CERTIFICATE_NAME}" \
     --certificate-body "file://${COMMON_NAME}_certificate.pem" \
     --private-key "file://${COMMON_NAME}.key"
   ```

1. Note the output

   ```
   {
       "ServerCertificateMetadata": {
           "Path": "/",
           "ServerCertificateName": "{certificate name}",
           "ServerCertificateId": "{server certificate id}",
           "Arn": "arn:aws:iam::{aws account number}:server-certificate/{certificate name}",
           "UploadDate": "YYYY-MM-DDThh:mm:ssZ",
           "Expiration": "YYYY-MM-DDThh:mm:ssZ"
       }
   }
   ```

1. View the server cerificate in certificate list

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" iam list-server-certificates
   ```

## Appendix YY: Review VictorOps documentation

### VictorOps Sources

1. Note "VictorOps" page in "MCT" Confluence space:*

   > https://confluence.cms.gov/pages/viewpage.action?spaceKey=MCT&title=VictorOps

### VictorOps Overview

1. Note the following about VictorOps

   - VictorOps is a tool to notify engineers and stakeholders of an incident

   - VictorOps provides a platform for incident reporting and escalation

1.  VictorOps configuration

   - define multiple team rotations based on need

   - define multiple escalations policies based on need

1. Defining teams

   - Engineering team

     - DevOps engineer

     - Backend developers

   - Product team

     - Scrum master

     - Business analyst

1. Defining escalation policies

   - 

1. Overview of a VictorOps process

   - VictorOps is notified of a problematic event

   - VictorOps pages on-call users (following a defined escalation policy) until someone is able to acknowledge the incident and begin investigating

## Appendix AAA: Upload static website to an Akamai Upload Directory within Akamai NetStorage

1. Change to the directory where the "_site" directory exists

   *Example where you are uploading the latest generated website:*

   ```ShellSession
   $ cd ~/code/ab2d/website
   ```

   *Example where the website directory was downloaded from S3:*

   ```ShellSession
   $ cd ~/akamai
   ```

1. Verify that that the website directory exists

   ```ShellSession
   $ [ -d "_site" ] \
     && echo -e "\nDirectory '_site' exists.\n" \
     || echo -e "\nError: Directory '_site' does not exist.\n"
   ```

1. Set a variable that points to your Akamai SSH key

   ```ShellSession
   $ NETSTORAGE_SSH_KEY="$HOME/.ssh/ab2d-akamai"
   ```

1. Set the target Akamai Upload Directory within Akamai Net Storage

   *Akamai Stage:*

   ```ShellSession
   $ AKAMAI_UPLOAD_DIRECTORY="971498"
   ```

1. Set the Akama Rsync domain

   ```ShellSession
   $ AKAMAI_RSYNC_DOMAIN=ab2d.rsync.upload.akamai.com
   ```

1. Set timestamp

   ```ShellSession
   $ TIMESTAMP=`date +%Y-%m-%d_%H-%M-%S`
   ```

1. Upload a timestamped website directory backup to the target Akamai Upload Directory

   ```ShellSession
   $ rsync \
     --progress \
     --partial \
     --archive \
     --verbose \
     --rsh="ssh -v -oStrictHostKeyChecking=no -oHostKeyAlgorithms=+ssh-dss -i ${NETSTORAGE_SSH_KEY}" \
     _site/* \
     "sshacs@${AKAMAI_RSYNC_DOMAIN}:/${AKAMAI_UPLOAD_DIRECTORY}/_site_${TIMESTAMP}"
   ```

1. Create or update the website directory in the target Akamai Upload Directory

   ```ShellSession
   $ rsync \
     --progress \
     --partial \
     --archive \
     --verbose \
     --force \
     --rsh="ssh -v -oStrictHostKeyChecking=no -oHostKeyAlgorithms=+ssh-dss -i ${NETSTORAGE_SSH_KEY}" \
     _site/* \
     "sshacs@${AKAMAI_RSYNC_DOMAIN}:/${AKAMAI_UPLOAD_DIRECTORY}/_site"
   ```

## Appendix BBB: Delete all files in an Akamai Upload Directory within Akamai NetStorage

1. Set a variable that points to your Akamai SSH key

   ```ShellSession
   $ NETSTORAGE_SSH_KEY="$HOME/.ssh/ab2d-akamai"
   ```

1. Set the target Akamai Upload Directory within Akamai Net Storage

   *Akamai Stage:*

   ```ShellSession
   $ AKAMAI_UPLOAD_DIRECTORY="971498"
   ```

1. Set the Akama Rsync domain

   ```ShellSession
   $ AKAMAI_RSYNC_DOMAIN=ab2d.rsync.upload.akamai.com
   ```

1. Create an empty directory to use with rsync

   ```ShellSession
   $ rm -rf /tmp/empty_dir \
     && mkdir /tmp/empty_dir
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Delete everything in target Akamai Upload Directory

   ```ShellSession
   $ rsync \
     --progress \
     --partial \
     --archive \
     --verbose \
     --omit-dir-times \
     --force \
     --rsh="ssh -v -oStrictHostKeyChecking=no -oHostKeyAlgorithms=+ssh-dss -i ${NETSTORAGE_SSH_KEY}" \
     --delete empty_dir/ "sshacs@${AKAMAI_RSYNC_DOMAIN}:/${AKAMAI_UPLOAD_DIRECTORY}/"
   ```

## Appendix CCC: Reconcile terraform state between two environments

### Reconcile terraform state of development environment with terraform state of implementation environment

1. Copy terraform state of the implementation environment to a file

   1. Open a new terminal tab

   1. Get credentials for the implementation environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Change to the implementation terraform environment directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl
      ```

   1. Copy terraform state to a file

      ```ShellSession
      $ terraform state list > ~/temp/impl.txt
      ```

1. Copy terraform state of the development environment to a file

   1. Open a new terminal tab

   1. Get credentials for the development environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Change to the development terraform environment directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-dev
      ```

   1. Copy terraform state to a file

      ```ShellSession
      $ terraform state list > ~/temp/dev.txt
      ```

1. Note any items that are in "dev.txt" file but not in "impl.txt" file

   *Example:*

   ```
   data.aws_db_instance.ab2d
   null_resource.authorized_keys_file
   module.controller.null_resource.deployment_contoller_private_key
   module.controller.null_resource.list-api-instances-script
   module.controller.null_resource.list-worker-instances-script
   module.controller.null_resource.pgpass
   module.controller.null_resource.remove_docker_from_controller
   module.controller.null_resource.set-hostname
   module.controller.null_resource.ssh_client_config
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::349849222861:policy/CMSApprovedAWSServices"]
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::349849222861:policy/CMSCloudApprovedRegions"]
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::349849222861:policy/ct-iamCreateUserRestrictionPolicy"]
   ```

1. Remove any items from dev that I no longer created by terraform

   1. Select the development environment terminal tab

   1. Refresh credentials for the development environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Remove the following from terraform state of development environment

      ```ShellSession
      $ terraform state rm data.aws_db_instance.ab2d
      $ terraform state rm null_resource.authorized_keys_file
      $ terraform state rm module.controller.null_resource.deployment_contoller_private_key
      $ terraform state rm module.controller.null_resource.list-api-instances-script
      $ terraform state rm module.controller.null_resource.list-worker-instances-script
      $ terraform state rm module.controller.null_resource.pgpass
      $ terraform state rm module.controller.null_resource.remove_docker_from_controller
      $ terraform state rm module.controller.null_resource.set-hostname
      $ terraform state rm module.controller.null_resource.ssh_client_config
      ```

1. Note any items that are in "impl.txt" file but not in "dev.txt" file

   *Example:*

   ```
   module.db.aws_security_group_rule.db_access_from_jenkins_agent
   module.kms.aws_kms_alias.a
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::330810004472:policy/CMSApprovedAWSServices"]
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::330810004472:policy/CMSCloudApprovedRegions"]
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::330810004472:policy/ct-iamCreateUserRestrictionPolicy"]
   ```

1. Add missing KMS alias

   1. Select the development environment terminal tab

   1. Refresh credentials for the development environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Import the missing KMS alias to put it under terraform management

      ```ShellSession
      $ terraform import module.kms.aws_kms_alias.a alias/ab2d-kms
      ```

1. Note that the following item that does not yet exist in development will be created when deploy-infrastructure.sh is run

   ```
   module.db.aws_security_group_rule.db_access_from_jenkins_agent
   ```

1. Run the following from Jenkins

   ```
   devops-engineer-only/01a-run-initialize-environment-only-for-development
   ```

1. Run the following from Jenkins

   ```
   devops-engineer-only/01b-run-update-gold-disk-only-for-development
   ```


1. Run the following from Jenkins

   ```
   devops-engineer-only/01c-run-deploy-infrastructure-only-for-development
   ```

1. Run the following from Jenkins

   ```
   01-update-application-only-for-development
   ```

1. Note the following error

   ```
   16:19:11 [1m[31mError: [0m[0m[1mError creating EFS file system: FileSystemAlreadyExists: File system 'fs-f7d2d376' already exists with creation token 'ab2d-dev-efs'
   16:19:11 {
   16:19:11   RespMetadata: {
   16:19:11     StatusCode: 409,
   16:19:11     RequestID: "3d0730d7-87ad-4cb6-b86d-170b0a30cffa"
   16:19:11   },
   16:19:11   ErrorCode: "FileSystemAlreadyExists",
   16:19:11   FileSystemId: "fs-f7d2d376",
   16:19:11   Message_: "File system 'fs-f7d2d376' already exists with creation token 'ab2d-dev-efs'"
   16:19:11 }[0m
   16:19:11 
   16:19:11 [0m  on ../../modules/efs/main.tf line 1, in resource "aws_efs_file_system" "efs":
   16:19:11    1: resource "aws_efs_file_system" "efs" [4m{[0m
   16:19:11 [0m
   16:19:11 [0m[0m
   16:19:11 Build step 'Execute shell' marked build as failure
   16:19:11 Finished: FAILURE
   ```

1. To fix the EFS error, do the following:

   1. Open the AWS Console

   1. Select **EFS**

   1. Select the radio button beside the following

      ```
      ab2d-dev-efs
      ```

   1. Select **Actions**

   1. Select **Delete file system**

   1. Enter the file id in the "Enter File System ID" text box

   1. Select **Delete File System**

   1. Wait for file system to delete

   1. Select **EC2**

   1. Select **Security Groups** from the leftmost panel

   1. Select the following security group

      ```
      ab2d-dev-efs-sg
      ```

   1. Select **Actions**

   1. Select **Delete security group**

   1. Select **Delete**

   1. Select the development terminal tab

   1. List and note EFS-related components that are currently in terraform state

      ```ShellSession
      $ terraform state list | grep efs
      ```

   1. Remove all listed EFS-related components from terraform state

      *Example:*

      ```ShellSession
      $ terraform state rm module.api.aws_security_group_rule.efs_ingress
      $ terraform state rm module.efs.aws_efs_file_system.efs
      $ terraform state rm module.efs.aws_security_group.efs
      $ terraform state rm module.worker.aws_security_group_rule.efs_ingress
      ```

   1. Re-run the following from Jenkins

      ```
      devops-engineer-only/01c-run-deploy-infrastructure-only-for-development
      ```

   1. Run the following from Jenkins

      ```
      01-update-application-only-for-development
      ```

### Reconcile terraform state of sandbox environment with terraform state of implementation environment

1. Copy terraform state of the implementation environment to a file

   1. Open a new terminal tab

   1. Get credentials for the implementation environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Change to the implementation terraform environment directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl
      ```

   1. Copy terraform state to a file

      ```ShellSession
      $ terraform state list > ~/temp/impl.txt
      ```

1. Copy terraform state of the sandbox environment to a file

   1. Open a new terminal tab

   1. Get credentials for the sandbox environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Change to the sandbox terraform environment directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox
      ```

   1. Copy terraform state to a file

      ```ShellSession
      $ terraform state list > ~/temp/sbx.txt
      ```

1. Note any items that are in "sbx.txt" file but not in "impl.txt" file

   *Example:*

   ```
   data.aws_db_instance.ab2d
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::777200079629:policy/CMSApprovedAWSServices"]
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::777200079629:policy/CMSCloudApprovedRegions"]
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::777200079629:policy/ct-iamCreateUserRestrictionPolicy"]   
   ```

1. Remove any items from sbx that I no longer created by terraform

   1. Select the sandbox environment terminal tab

   1. Refresh credentials for the sandbox environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Remove the following from terraform state of sandbox environment

      ```ShellSession
      $ terraform state rm data.aws_db_instance.ab2d
      ```

1. Note any items that are in "impl.txt" file but not in "sbx.txt" file

   *Example:*

   ```
   module.api.data.aws_security_group.cms_cloud_vpn
   module.api.aws_security_group_rule.cms_cloud_vpn_access
   module.controller.aws_eip.deployment_controller
   module.controller.aws_instance.deployment_controller
   module.controller.aws_security_group.deployment_controller
   module.controller.aws_security_group_rule.db_access_from_controller
   module.controller.aws_security_group_rule.egress_controller
   module.controller.aws_security_group_rule.vpn_access_controller
   module.controller.null_resource.wait
   module.controller.random_shuffle.public_subnets
   module.db.aws_db_instance.db
   module.db.aws_db_parameter_group.default
   module.db.aws_db_subnet_group.subnet_group
   module.db.aws_security_group.sg_database
   module.db.aws_security_group_rule.db_access_from_jenkins_agent
   module.db.aws_security_group_rule.egress
   module.iam.aws_iam_instance_profile.test_profile
   module.iam.aws_iam_policy.cloud_watch_logs_policy
   module.iam.aws_iam_policy.packer_policy
   module.iam.aws_iam_policy.s3_access_policy
   module.iam.aws_iam_role.ab2d_instance_role
   module.iam.aws_iam_role.ab2d_mgmt_role
   module.iam.aws_iam_role_policy_attachment.amazon_ec2_container_service_for_ec2_role_attach
   module.iam.aws_iam_role_policy_attachment.cms_approved_aws_services_attach
   module.iam.aws_iam_role_policy_attachment.instance_role_bfd_opt_out_policy_attach
   module.iam.aws_iam_role_policy_attachment.instance_role_cloud_watch_logs_policy_attach
   module.iam.aws_iam_role_policy_attachment.instance_role_packer_policy_attach
   module.iam.aws_iam_role_policy_attachment.instance_role_s3_access_policy_attach
   module.kms.data.aws_iam_policy_document.instance_role_kms_policy
   module.kms.aws_iam_policy.kms_policy
   module.kms.aws_iam_role_policy_attachment.instance_role_kms_policy_attach
   module.kms.aws_kms_alias.a
   module.kms.aws_kms_key.a
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::330810004472:policy/CMSApprovedAWSServices"]
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::330810004472:policy/CMSCloudApprovedRegions"]
   module.management_target.aws_iam_role_policy_attachment.mgmt_role_assume_policy_attach["arn:aws:iam::330810004472:policy/ct-iamCreateUserRestrictionPolicy"]
   ```

1. Add missing IAM terraform components that already exist

   1. Select the sandbox environment terminal tab

   1. Refresh credentials for the sandbox environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Import the missing IAM components to put it under terraform management

      ```ShellSession
      $ terraform import module.iam.aws_iam_role.ab2d_instance_role Ab2dInstanceRole
      $ terraform import module.iam.aws_iam_role.ab2d_mgmt_role Ab2dMgmtRole
      $ terraform import module.iam.aws_iam_instance_profile.test_profile Ab2dInstanceProfile
      $ terraform import module.iam.aws_iam_policy.cloud_watch_logs_policy arn:aws:iam::777200079629:policy/Ab2dCloudWatchLogsPolicy
      $ terraform import module.iam.aws_iam_policy.packer_policy arn:aws:iam::777200079629:policy/Ab2dPackerPolicy
      $ terraform import module.iam.aws_iam_policy.s3_access_policy arn:aws:iam::777200079629:policy/Ab2dS3AccessPolicy
      $ terraform import module.iam.aws_iam_role_policy_attachment.amazon_ec2_container_service_for_ec2_role_attach Ab2dInstanceRole/arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role
      $ terraform import module.iam.aws_iam_role_policy_attachment.instance_role_cloud_watch_logs_policy_attach Ab2dInstanceRole/arn:aws:iam::777200079629:policy/Ab2dCloudWatchLogsPolicy
      $ terraform import module.iam.aws_iam_role_policy_attachment.instance_role_packer_policy_attach Ab2dInstanceRole/arn:aws:iam::777200079629:policy/Ab2dPackerPolicy
      $ terraform import module.iam.aws_iam_role_policy_attachment.instance_role_s3_access_policy_attach Ab2dInstanceRole/arn:aws:iam::777200079629:policy/Ab2dS3AccessPolicy

      ??????????
      $ terraform import module.iam.aws_iam_role_policy_attachment.cms_approved_aws_services_attach
      ??????????
      $ terraform import module.iam.aws_iam_role_policy_attachment.instance_role_bfd_opt_out_policy_attach
      ```

1. Add missing KMS components

   1. Select the sandbox environment terminal tab

   1. Refresh credentials for the sandbox environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Import the missing KMS components to put it under terraform management

      ```ShellSession
      $ KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
        --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
        --output text)
      $ terraform import module.kms.aws_kms_key.a "${KMS_KEY_ID}"
      $ terraform import module.kms.aws_kms_alias.a alias/ab2d-kms
      $ KMS_IAM_POLICY_ARN=$(aws --region "${AWS_DEFAULT_REGION}" iam list-policies \
        --query "Policies[?PolicyName=='Ab2dKmsPolicy'].Arn" \
        --output text)
      $ terraform import module.kms.aws_iam_policy.kms_policy "${KMS_IAM_POLICY_ARN}"
      $ terraform import module.kms.aws_iam_role_policy_attachment.instance_role_kms_policy_attach Ab2dInstanceRole/arn:aws:iam::777200079629:policy/Ab2dKmsPolicy

      ???????????   
      module.kms.data.aws_iam_policy_document.instance_role_kms_policy
      ```

1. Add missing db components

   1. Select the sandbox environment terminal tab

   1. Refresh credentials for the sandbox environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Import the missing db components to put it under terraform management

      ```ShellSession
      $ AB2D_DATABASE_SG=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-security-groups \
        --query "SecurityGroups[?GroupName=='ab2d-database-sg'].GroupId" \
        --output text)
      $ terraform import module.db.aws_security_group.sg_database "${AB2D_DATABASE_SG}"
      $ terraform import module.db.aws_db_subnet_group.subnet_group ab2d-rds-subnet-group
      $ terraform import module.db.aws_db_parameter_group.default ab2d-rds-parameter-group
      $ terraform import module.db.aws_security_group_rule.egress "${AB2D_DATABASE_SG}_egress_all_0_65536_0.0.0.0/0"
      $ terraform import module.db.aws_db_instance.db ab2d
      ```

1. Note that the following item that does not yet exist in development will be created when deploy-infrastructure.sh is run

   ```
   module.db.aws_security_group_rule.db_access_from_jenkins_agent
   ```

1. Add missing controller components

   1. Select the sandbox environment terminal tab

   1. Refresh credentials for the sandbox environment

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Import the missing controller components to put it under terraform management

      ```ShellSession
      $ AB2D_DEPLOYMENT_CONTROLLER_SG=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-security-groups \
  --query "SecurityGroups[?GroupName=='ab2d-deployment-controller-sg'].GroupId" \
  --output text)
      $ terraform import module.controller.aws_security_group.deployment_controller "${AB2D_DEPLOYMENT_CONTROLLER_SG}"
      $ terraform import module.controller.aws_eip.deployment_controller eipalloc-066af1aaceecd5b70
      $ terraform import module.controller.aws_security_group_rule.db_access_from_controller sg-0e0f230b719384463_ingress_tcp_5432_5432_sg-0b2eaf3ed92a7448d
      $ terraform import module.controller.aws_security_group_rule.vpn_access_controller sg-0b2eaf3ed92a7448d_ingress_all_0_65536_10.232.32.0/19
      $ terraform import module.controller.aws_security_group_rule.egress_controller sg-0b2eaf3ed92a7448d_egress_all_0_65536_0.0.0.0/0
      ```

1. Remove auto-added secuity group rules

   ```ShellSession
   $ terraform state rm module.db.aws_security_group_rule.sg_database
   $ terraform state rm module.db.aws_security_group_rule.sg_database-1
   $ terraform state rm module.db.aws_security_group_rule.sg_database-2
   $ terraform state rm module.db.aws_security_group_rule.sg_database-3
   $ terraform state rm module.controller.aws_security_group_rule.deployment_controller
   $ terraform state rm module.controller.aws_security_group_rule.deployment_controller-1
   $ terraform state rm module.controller.aws_security_group_rule.deployment_controller-2
   ```

1. Manually add a "CMS Cloud VPN Access" ingress rule to the "ab2d-east-prod-load-balancer-sg" security group with the following settings in the AWS console

   ```
   All traffic
   All
   All
   sg-0ba0fa6edce74b231 (cmscloud-vpn)
   CMS Cloud VPN Access
   ```

1. Import the "CMS Cloud VPN Access" ingress rule into terraform state

   ```ShellSession
   $ terraform import module.api.aws_security_group_rule.cms_cloud_vpn_access sg-0372341978a0c7caf_ingress_all_0_65536_sg-0ba0fa6edce74b231
   ```

1. Manually add a "Jenkins Agent Access" ingress rule to the "ab2d-database-sg" security group with the following settings in the AWS console

   ```
   PostgreSQL
   TCP
   5432
   Custom
   653916833532/sg-0e370f9dcfe051ed0
   Jenkins Agent Access
   ```

1. Import the "Jenkins Agent Access" ingress rule into terraform state

   ```ShellSession
   $ terraform import module.db.aws_security_group_rule.db_access_from_jenkins_agent sg-0e0f230b719384463_ingress_tcp_5432_5432_653916833532/sg-0e370f9dcfe051ed0
   ```

1. Run "deploy-infrastructure.sh" from Jenkins for the sandbox environment

   > *** TO DO ***

## Appendix DDD: Backup and recovery

1. Stop API nodes via an autoscaling group schedule

   1. Log on to the target AWS account

   1. Select **EC2**

   1. Select **Auto Scaling Groups** form the leftmost panel

   1. Select the API auto scaling group

      *Example:*

      ```
      ab2d-sbx-sandbox-api-20200414203112829500000001
      ```

   1. Select the **Schedule Actions** tab

   1. Select **Create Scheduled Action**

   1. Configure the "Create Scheduled Action" page as follows

      *Format:*

      - **Name:** Stop API nodes

      - **Min:** 0

      - **Max:** 0

      - **Desired Capacity:** 0

      - **Recurrence:** Once

      - **Start Time:** {utc time two minutes from now}

1. Stop worker nodes via an autoscaling group schedule

   1. Log on to the target AWS account

   1. Select **EC2**

   1. Select **Auto Scaling Groups** form the leftmost panel

   1. Select the API auto scaling group

      *Example:*

      ```
      ab2d-sbx-sandbox-worker-20200414203214104900000001
      ```

   1. Select the **Schedule Actions** tab

   1. Select **Create Scheduled Action**

   1. Configure the "Create Scheduled Action" page as follows

      *Format:*

      - **Name:** Stop worker nodes

      - **Min:** 0

      - **Max:** 0

      - **Desired Capacity:** 0

      - **Recurrence:** Once

      - **Start Time:** {utc time two minutes from now}

1. Wait for API and worker nodes to stop

1. Take a database snapshot

   1. Log on to the target AWS account

   1. Select **RDS**

   1. Select **Databases** from the leftmost panel

   1. Select the radio button beside the following database

      ```
      ab2d
      ```

   1. Select **Actions**

   1. Select **Take snapshot**

   1. Type the following in the **Snapshot name** text box

      *Format:*

      ```
      ab2d-{unix time}
      ```

   1. Select **Take Snapshot**

   1. Wait for "Snapshot creation time" to appear

      *Notes:*

      - Note that this may take a while

      - Note that if you select the snapshot you will see the status as "Creating", which is why the snapshot creation time has not yet been updated

      - Note that you may need to refresh the page to see the snapshot creation time

1. Backup the current state of data by running the following Jenkins job

   ```
   devops-engineer-only/02-backup-data-as-csv-for-sandbox
   ```

1. Make a backup of CSV backup files

   1. Get credentials for the management AWS account

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Get the private IP address of Jenkins agent instance

      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Connect to the Jenkins agent

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_PRIVATE_IP}"
      ```

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. List the csv files

      *Example for the "Sbx" environment:*

      ```ShellSession
      $ ls -al ~/database_backup/ab2d-sbx-sandbox/csv
      ```

   1. Backup the csv directory

      *Example for the "Sbx" environment:*

      ```ShellSession
      $ cd ~
      $ DATETIME=`date +%Y-%m-%d-%H%M%S`
      $ mkdir -p "csv-backup/${DATETIME}"
      $ cp -r database_backup/ab2d-sbx-sandbox/csv "csv-backup/${DATETIME}"
      ```

1. Run the initialize environment Jenkins job

   ```
   devops-engineer-only/01a-run-initialize-environment-only-for-sandbox
   ```

1. Run the update gold disk Jenkins job

   ```
   devops-engineer-only/01b-run-update-gold-disk-only-for-sandbox
   ```

1. Run the deploy infrastructre Jenkins job

   ```
   devops-engineer-only/01c-run-deploy-infrastructure-only-for-sandbox
   ```

1. Run the update application Jenkins job

   ```
   devops-engineer-only/01-update-application-only-for-sandbox
   ```

## Appendix EEE: Modify the database instance type

1. Stop API nodes via an autoscaling group schedule

   1. Log on to the target AWS account

   1. Select **EC2**

   1. Select **Auto Scaling Groups** form the leftmost panel

   1. Select the API auto scaling group

      *Example:*

      ```
      ab2d-sbx-sandbox-api-20200414203112829500000001
      ```

   1. Select the **Schedule Actions** tab

   1. Select **Create Scheduled Action**

   1. Configure the "Create Scheduled Action" page as follows

      *Format example for "Sbx" environment:*

      - **Name:** Stop API nodes

      - **Min:** 0

      - **Max:** 0

      - **Desired Capacity:** 0

      - **Recurrence:** Once

      - **Start Time:** {utc time two minutes from now}

1. Stop worker nodes via an autoscaling group schedule

   1. Log on to the target AWS account

   1. Select **EC2**

   1. Select **Auto Scaling Groups** form the leftmost panel

   1. Select the API auto scaling group

      *Example:*

      ```
      ab2d-sbx-sandbox-worker-20200414203214104900000001
      ```

   1. Select the **Schedule Actions** tab

   1. Select **Create Scheduled Action**

   1. Configure the "Create Scheduled Action" page as follows

      *Format example for "Sbx" environment:*

      - **Name:** Stop worker nodes

      - **Min:** 0

      - **Max:** 0

      - **Desired Capacity:** 0

      - **Recurrence:** Once

      - **Start Time:** {utc time two minutes from now}

1. Wait for API and worker nodes to stop

1. Take a database snapshot

   1. Log on to the target AWS account

   1. Select **RDS**

   1. Select **Databases** from the leftmost panel

   1. Select the radio button beside the following database

      ```
      ab2d
      ```

   1. Select **Actions**

   1. Select **Take snapshot**

   1. Type the following in the **Snapshot name** text box

      *Format:*

      ```
      ab2d-{unix time}
      ```

   1. Select **Take Snapshot**

   1. Wait for "Snapshot creation time" to appear

      *Notes:*

      - Note that this may take a while

      - Note that if you select the snapshot you will see the status as "Creating", which is why the snapshot creation time has not yet been updated

      - Note that you may need to refresh the page to see the snapshot creation time

1. Backup the current state of data by running the following Jenkins job

   ```
   devops-engineer-only/02-backup-data-as-csv-for-sandbox
   ```

1. Make a backup of CSV backup files

   1. Get credentials for the management AWS account

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Get the private IP address of Jenkins agent instance

      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Connect to the Jenkins agent

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_PRIVATE_IP}"
      ```

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. List the csv files

      *Example for the "Sbx" environment:*

      ```ShellSession
      $ ls -al ~/database_backup/ab2d-sbx-sandbox/csv
      ```

   1. Backup the csv directory

      *Example for the "Sbx" environment:*

      ```ShellSession
      $ cd ~
      $ DATETIME=`date +%Y-%m-%d-%H%M%S`
      $ mkdir -p "csv-backup/${DATETIME}"
      $ cp -r database_backup/ab2d-sbx-sandbox/csv "csv-backup/${DATETIME}"
      ```

1. Remove the database from terraform management prior to modifying database instance

   1. Switch to the sandbox directory

      *Example for "Sbx" environment:*

      ```ShellSession
      $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox
      ```

   1. Get credentials for the sandbox environment
   
      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```
   
   1. Remove the database terraform module
   
      ```ShellSession
      $ terraform state rm module.db.aws_db_instance.db
      ```

1. Modify the database instance in the AWS console

   1. Log on to the target AWS account

   1. Select **RDS**

   1. Select **Databases** from the leftmost panel

   1. Select the radio button beside the following database

      ```
      ab2d
      ```

   1. Select **Modify**

   1. Modify the database as follows

      - **DB engine version:** PostgreSQL 11.5-R1

      - **DB instance class:** db.m4.2xlarge

      - **Multi-AZ deployment:** Yes

      - **Storage type:** Provisioned IOPS (SSD)

      - **Allocated Storage:** 500 GiB

      - **Provisioned IOPS:** 5000

      - **DB instance identifier:** ab2d

   1. Scroll to the bottom of the page

   1. Select **Continue**

   1. Select the **Apply immediately** radio button

   1. Select **Modify DB Instance**

1. Wait for the database modification to complete

   *Notes:*

   - it may take a short period of time to see the status change to "Modifying"

   - you may need to refresh the page to see the status changes

1. Backup the current state of data by running the following Jenkins job again

   ```
   devops-engineer-only/02-backup-data-as-csv-for-sandbox
   ```

1. Import the new database configuration into terraform

   1. Switch to the sandbox directory

      *Example for "Sbx" environment:*

      ```ShellSession
      $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox
      ```

   1. Get credentials for the sandbox environment
   
      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Import the latest database instance into terraform

      ```ShellSession
      $ terraform import module.db.aws_db_instance.db ab2d
      ```

1. Restart the API nodes

   1. Log on to the target AWS account

   1. Select **EC2**

   1. Select **Auto Scaling Groups** form the leftmost panel

   1. Select the API auto scaling group

      *Example:*

      ```
      ab2d-sbx-sandbox-api-20200414203112829500000001
      ```

   1. Select the **Schedule Actions** tab

   1. Select **Create Scheduled Action**

   1. Configure the "Create Scheduled Action" page as follows

      *Format example for "Sbx" environment:*

      - **Name:** Start API nodes

      - **Min:** 2

      - **Max:** 2

      - **Desired Capacity:** 2

      - **Recurrence:** Once

      - **Start Time:** {utc time two minutes from now}

1. Restart the worker nodes

   1. Log on to the target AWS account

   1. Select **EC2**

   1. Select **Auto Scaling Groups** form the leftmost panel

   1. Select the API auto scaling group

      *Example:*

      ```
      ab2d-sbx-sandbox-worker-20200414203214104900000001
      ```

   1. Select the **Schedule Actions** tab

   1. Select **Create Scheduled Action**

   1. Configure the "Create Scheduled Action" page as follows

      *Format example for "Sbx" environment:*

      - **Name:** Start worker nodes

      - **Min:** 2

      - **Max:** 2

      - **Desired Capacity:** 2

      - **Recurrence:** Once

      - **Start Time:** {utc time two minutes from now}

## Appendix FFF: Run e2e tests

### Run e2e tests for development

1. Add "dev.ab2d.cms.gov" mapping to hosts file as a temporary workaround until domain is onboarded to Akamai

   1. Change to the "bash" directory

      ```ShellSession
      $ cd ~/code/ab2d/Deploy/bash
      ```

   1. Set the target environment

      ```ShellSession
      $ source ./set-env.sh
      ```

   1. Determine the line to be added to hosts file

      ```ShellSession
      $ aws --region us-east-1 ec2 describe-network-interfaces \
        --filters Name=requester-id,Values=amazon-elb \
        --query "NetworkInterfaces[*].PrivateIpAddress" \
        --output json \
        | jq '.[0]' \
        | tr -d '"' \
        | awk '{print $1" dev.ab2d.cms.gov"}'
      ```

   1. Note the dev.ab2d.cms.gov domain mapping line that is output

      *Format:*

      ```
      {one load balancer private ip address} dev.ab2d.cms.gov
      ```

   1. Open the hosts file

      ```ShellSession
      $ sudo vim /etc/hosts
      ```

   1. Add the following line to the hosts file

      ```
      {one load balancer private ip address} dev.ab2d.cms.gov
      ```

   1. Save and close the file

1. Switch to the repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set configuration prefix

   ```ShellSession
   $ export E2E_ENV_CONFIG_PREFIX=dev
   ```

1. Set e2e test environment target

   ```ShellSession
   $ export E2E_TARGET_ENV=DEV
   ```

1. Examine the configuration file

   ```ShellSession
   $ cat "./e2e-test/src/test/resources/${E2E_ENV_CONFIG_PREFIX}-config.yml" ; echo
   ```

1. If the settings are not correct, do the following

   1. Open the configuration file

      ```ShellSession
      $ vim "./e2e-test/src/test/resources/${E2E_ENV_CONFIG_PREFIX}-config.yml"
      ```

   1. Modify the settings to make them correct

   1. Save and close the file

1. Build the application and skip tests

   ```ShellSession
   $ mvn clean package -DskipTests
   ```

1. Set first OKTA test user

   *NOTE: currently using a real Okta contract with a small number of patients since the synthetic data is not working.*

   ```ShellSession
   $ export OKTA_CONTRACT_NUMBER={okta contract number}
   $ export OKTA_CLIENT_ID={first okta client id}
   $ export OKTA_CLIENT_PASSWORD={first okta client secret}
   ```

1. Set second OKTA test user

   *NOTE: currently using a real Okta contract with a small number of patients since the synthetic data is not working.*

   ```ShellSession
   $ export SECONDARY_USER_OKTA_CLIENT_ID={second okta client id}
   $ export SECONDARY_USER_OKTA_CLIENT_PASSWORD={second okta client secret}
   ```

1. Change to the target directory

   ```ShellSession
   $ cd ./e2e-test/target
   ```

1. Run e2e testing

   ```ShellSession
   $ java -cp e2e-test-0.0.1-SNAPSHOT-fat-tests.jar gov.cms.ab2d.e2etest.TestLauncher "${E2E_TARGET_ENV}" "${OKTA_CONTRACT_NUMBER}"
   ```

### Run e2e tests for sandbox

1. Switch to the repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set configuration prefix

   ```ShellSession
   $ export E2E_ENV_CONFIG_PREFIX=sbx
   ```

1. Set e2e test environment target

   ```ShellSession
   $ export E2E_TARGET_ENV=SBX
   ```

1. Examine the configuration file

   ```ShellSession
   $ cat "./e2e-test/src/test/resources/${E2E_ENV_CONFIG_PREFIX}-config.yml" ; echo
   ```

1. If the settings are not correct, do the following

   1. Open the configuration file

      ```ShellSession
      $ vim "./e2e-test/src/test/resources/${E2E_ENV_CONFIG_PREFIX}-config.yml"
      ```

   1. Modify the settings to make them correct

   1. Save and close the file

1. Build the application and skip tests

   ```ShellSession
   $ mvn clean package -DskipTests
   ```

1. Set first OKTA test user

   *NOTE: currently using a real Okta contract with a small number of patients since the synthetic data is not working.*

   ```ShellSession
   $ export OKTA_CONTRACT_NUMBER={okta contract number}
   $ export OKTA_CLIENT_ID={first okta client id}
   $ export OKTA_CLIENT_PASSWORD={first okta client secret}
   ```

1. Set second OKTA test user

   *NOTE: currently using a real Okta contract with a small number of patients since the synthetic data is not working.*

   ```ShellSession
   $ export SECONDARY_USER_OKTA_CLIENT_ID={second okta client id}
   $ export SECONDARY_USER_OKTA_CLIENT_PASSWORD={second okta client secret}
   ```

1. Change to the target directory

   ```ShellSession
   $ cd ./e2e-test/target
   ```

1. Run e2e testing

   ```ShellSession
   $ java -cp e2e-test-0.0.1-SNAPSHOT-fat-tests.jar gov.cms.ab2d.e2etest.TestLauncher "${E2E_TARGET_ENV}" "${OKTA_CONTRACT_NUMBER}"
   ```

### Run e2e tests for production

1. Switch to the repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set configuration prefix

   ```ShellSession
   $ export E2E_ENV_CONFIG_PREFIX=prod
   ```

1. Set e2e test environment target

   ```ShellSession
   $ export E2E_TARGET_ENV=PROD
   ```

1. Examine the configuration file for production

   ```ShellSession
   $ cat "./e2e-test/src/test/resources/${E2E_ENV_CONFIG_PREFIX}-config.yml" ; echo
   ```

1. If the settings are not correct, do the following

   1. Open the configuration file

      ```ShellSession
      $ vim "./e2e-test/src/test/resources/${E2E_ENV_CONFIG_PREFIX}-config.yml"
      ```

   1. Modify the settings to make them correct

   1. Save and close the file

1. Build the application and skip tests

   ```ShellSession
   $ mvn clean package -DskipTests
   ```

1. Set first OKTA test user

   *NOTE: currently using a real Okta contract with a small number of patients since the synthetic data is not working.*

   ```ShellSession
   $ export OKTA_CONTRACT_NUMBER={okta contract number}
   $ export OKTA_CLIENT_ID={first okta client id}
   $ export OKTA_CLIENT_PASSWORD={first okta client secret}
   ```

1. Set second OKTA test user

   *NOTE: currently using a real Okta contract with a small number of patients since the synthetic data is not working.*

   ```ShellSession
   $ export SECONDARY_USER_OKTA_CLIENT_ID={second okta client id}
   $ export SECONDARY_USER_OKTA_CLIENT_PASSWORD={second okta client secret}
   ```

1. Change to the target directory

   ```ShellSession
   $ cd ./e2e-test/target
   ```

1. Run e2e testing

   ```ShellSession
   $ java -cp e2e-test-0.0.1-SNAPSHOT-fat-tests.jar gov.cms.ab2d.e2etest.TestLauncher "${E2E_TARGET_ENV}" "${OKTA_CONTRACT_NUMBER}"
   ```

## Appendix GGG: Retrieve a copy of remote terraform state file for review

1. Ensure that you are connected to CMS Cisco VPN

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set AWS environment variables using the CloudTamer API

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Choose desired target environment

1. Change to target directory of the desired target environment

   ```ShellSession
   $ cd terraform/environments/ab2d-east-prod
   ```

1. Pull the terraform state file for the target environment from S3

   ```ShellSession
   $ terraform state pull > "${HOME}/Downloads/terraform.tfstate.${CMS_ENV}.json"
   ```

1. Examine the downloaded terraform state file

   ```ShellSession
   $ vim "${HOME}/Downloads/terraform.tfstate.${CMS_ENV}.json"
   ```

## Appendix HHH: Manually change a tag on controller and update its terraform state

1. Ensure that you have added or changed the tag in the automation for the controller before proceeding

1. Ensure that you are connected to CMS Cisco VPN

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set AWS environment variables using the CloudTamer API

   ```ShellSession
   $ source ~/code/ab2d/Deploy/bash/set-env.sh
   ```

1. Choose desired target environment

1. Change to target directory of the desired target environment

   ```ShellSession
   $ cd terraform/environments/ab2d-east-prod
   ```

1. Get the terraform name for the controller's AWS instance within the terraform state

   ```ShellSession
   $ TERRAFORM_CONTROLLER_AWS_INSTANCE=$(terraform state list \
     | grep module.controller \
     | grep aws_instance)
   ```

1. Verify that one and one item is in the "TERRAFORM_CONTROLLER_AWS_INSTANCE" variable

   ```ShellSession
   $ echo "${TERRAFORM_CONTROLLER_AWS_INSTANCE}"
   ```

1. Temporarily remove the controller from terraform management

   ```ShellSession
   $ terraform state rm "${TERRAFORM_CONTROLLER_AWS_INSTANCE}"
   ```

1. Open Chrome

1. Enter the following in the address bar

   > https://cloudtamer.cms.gov/portal

1. Select the target AWS account

1. Manually change one of the tags associated with the instance

   1. Select **EC2**

   1. Select **Instances** under he "Instances" section from the leftmost panel

   1. Select the following EC2 instance

      ```
      ab2d-deployment-controller
      ```

   1. Select the **Tags** tab

   1. Add or configure a tag as desired

      *Example:*

      Tags      |Value
      ----------|--------
      cpm backup|no-backup

1. Return to the terminal

1. Import the controller to terraform state to place it under terraform management again

   1. Get the instance id of the terraform controller

      ```ShellSession
      $ CONTROLLER_INSTANCE_ID=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
        --query="Reservations[*].Instances[*].InstanceId" \
        --output text)
      ```

   1. Import the controller

      ```ShellSession
      $ terraform import "${TERRAFORM_CONTROLLER_AWS_INSTANCE}" "${CONTROLLER_INSTANCE_ID}"
      ```

1. Note that even though terraform state did not update to reflect the tag change, this is still a good example for how to put existing infrastructure under terraform management

## Appendix III: Issue a schedule override in VictorOps

1. Open Chrome

1. Log on to VictorOps

1. Select the **Teams** tab

1. Select **On-Call Schedule**

1. Expand **Standard**

1. Note that the schedule for the next four weeks is displayed

1. If there are no existing overrides, all the lines are a shades of gray-blue

1. Determine when the override will occur

1. Select the **Scheduled Overrides** tab

1. Select **Schedule an Override**

1. Configure the "Create Scheduled" page as follows

   - **Override for:** {user whose schedule will be overriden}

   - **Timezone:** America/New York

   - **Start on:** {datetime} <-- musst be in the future

   - **End on:** {datetime}

1. Select **Create**

1. Expand the override that was created

1. Select the user from the dropdown that will take the override time period

## Appendix JJJ: Change the Jenkins home directory on the Jenkins agent

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Determine the configured Jenkins home directory from the Jenkins GUI

   1. Select **Manage Jenkins**

   1. Scroll down to the "Status Information" section

   1. Select **System Information**

   1. Scroll down to "JENKINS_HOME"

   1. Note the "JENKINS_HOME" value

      ```
      /var/lib/jenkins
      ```

1. Note that Jenkins Master currently uses the same value as its Jenkins home

   ```
   /var/lib/jenkins
   ```

1. Note that Jenkins Agent currently uses a different value as its Jenkins home

   ```
   /home/jenkins
   ```

1. Note that the latest SSH plugin requires that the Jenkins home directory should be the same on both Jenkins master and Jenkins agent, so the following directions will change the Jenkins agent home to be the same as Jenkins master

1. Double the size of the Jenkins agent volume in AWS

   1. Note that this is needed to fix the size of the "/var" volume so that it has enough room to receive the existing file from the old jenkins home directory

   1. Log on the the mangement AWS account

   1. Select **EC2**

   1. Select **Volumes** under "Elastic Block Store" in the leftmost panel

   1. Select the following volume

      ```
      ab2d-jenkins-agent-vol
      ```

   1. Select **Actions**

   1. Select **Modify Volume**

   1. Type the following in the **Size** text box

      ```
      500
      ```

   1. Select **Modify**

   1. Select **Yes** on the "Are you sure..." dialog

   1. Select **Close** on the "Modify Volume" window

1. SSH into the Jenkins agent

1. Create a new partition from unallocated space

   ```ShellSession
   (
   echo n # Add a new partition
   echo p # Primary partition
   echo   # Partition number (Accept default)
   echo   # First sector (Accept default)
   echo   # Last sector (Accept default)
   echo w # Write changes
   ) | sudo fdisk /dev/nvme0n1 || true
   ```

1. Request that the operating system re-reads the partition table

   ```ShellSession
   $ sudo partprobe
   ```

1. Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

   ```ShellSession
   $ sudo pvcreate /dev/nvme0n1p4
   ```

1. Add the new physical volume to the volume group

   ```ShellSession
   $ sudo vgextend VolGroup00 /dev/nvme0n1p4
   ```

1. Extend the size of the var logical volume

   ```ShellSession
   $ sudo lvextend -l +100%FREE /dev/mapper/VolGroup00-varVol
   ```

1. Expand the existing XFS filesystem

   ```ShellSession
   $ sudo xfs_growfs -d /dev/mapper/VolGroup00-varVol
   ```

1. Switch to the root user's "/root" directory

   ```ShellSession
   $ sudo -i
   ```

1. Create the new Jenkins home directory

   ```ShellSession
   $ mkdir -p /var/lib/jenkins
   ```

1. Change the ownership of the new Jenkins home directory to the jenkins user

   ```ShellSession
   $ chown jenkins:jenkins /var/lib/jenkins
   ```

1. Copy the contents from old Jenkins home directory to the new Jenkins home directory

   ```ShellSession
   $ cp -prv /home/jenkins /var/lib/
   ```

1. Change the Jenkins user home directory

   ```ShellSession
   $ usermod -d /var/lib/jenkins jenkins
   ```

## Appendix KKK: Change MFA to Google Authenticator for accessing Jira

1. Install Google Authenticator on your phone (if not already installed)

1. Open Chrome

1. Enter the following in the address bar

   > https://mo-idp.cms.gov/login#showall

1. Log on using your current MFA

1. Select **MFA Authentication** from the leftmost panel

1. Select **Google Authenticator**

1. Follow the instructions that are presented

## Appendix LLL: Add a volume to jenkins agent and extend the log volume to use it

1. Create a new volume in the same availabilty zone as Jenkins agent in the AWS console

1. Attach the volume to the Jenkins agent in the AWS console

1. Connect to the Jenkins agent

1. Set the partition as gpt

   ```ShellSession
   $ sudo parted --script /dev/nvme1n1 mklabel gpt
   ```

1. View detail about the disks and partitions again

   1. Enter the following
   
      ```ShellSession
      $ sudo parted -l
      ```

   1. Note the output

      ```
      Model: NVMe Device (nvme)
      Disk /dev/nvme0n1: 537GB
      Sector size (logical/physical): 512B/512B
      Partition Table: gpt
      Disk Flags: 
      
      Number  Start   End     Size    File system  Name  Flags
       1      1049kB  2097kB  1049kB                     bios_grub
       2      2097kB  537GB   537GB   xfs
      
      
      Model: NVMe Device (nvme)
      Disk /dev/nvme1n1: 537GB
      Sector size (logical/physical): 512B/512B
      Partition Table: gpt
      Disk Flags: 
      
      Number  Start  End  Size  File system  Name  Flags
      ```

1. Create a new partition on the "/dev/nvme1n1" disk

   ```ShellSession
   (
   echo n # Add a new partition
   echo p # Primary partition
   echo   # Partition number (Accept default)
   echo   # First sector (Accept default)
   echo   # Last sector (Accept default)
   echo w # Write changes
   ) | sudo fdisk /dev/nvme1n1
   ```

1. Request that the operating system re-reads the partition table

   ```ShellSession
   $ sudo partprobe
   ```

1. Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

   ```ShellSession
   $ sudo pvcreate /dev/nvme1n1p1
   ```

1. Format the "/dev/nvme1n1" disk as xfs

   ```ShellSession
   $ sudo mkfs.xfs -f /dev/nvme1n1p1
   ```

1. Extend the "VolGroup00" volume group to include the new volume

   ```ShellSession
   $ sudo vgextend VolGroup00 /dev/nvme1n1p1
   ```

1. Extend the size of the "log" logical volume with all the free space on the new volume

   ```ShellSession
   $ sudo lvextend -l +100%FREE /dev/mapper/VolGroup00-logVol
   ```

1. Expands the existing XFS filesystem

   ```ShellSession
   $ sudo xfs_growfs -d /dev/mapper/VolGroup00-logVol
   ```

## Appendix MMM: Upgrade Jenkins Agent from AWS CLI 1 to AWS CLI 2

### Uninstall AWS CLI 1 using pip

1. Connect to the Jenkins agent

1. Uninstall AWS CLI 1 using pip

   ```ShellSession
   $ sudo pip uninstall -y awscli
   ```

### Install and verify AWS CLI 2

1. Connect to the Jenkins agent

1. Download the AWS CLI 2 package

   ```ShellSession
   $ cd /tmp
   $ curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
   ```

1. Unzip the AWS CLI 2 package

   ```ShellSession
   $ unzip awscliv2.zip
   ```

1. Install AWS CLI 2

   ```ShellSession
   $ sudo ./aws/install
   ```

1. Verify where AWS CLI 2 was installed

   1. Enter the following

      ```ShellSession
      $ which aws
      ```

   1. Verify that the following was displayed

      ```
      /usr/local/bin/aws
      ```

1. Verify the version of AWS CLI 2

   1. Enter the following

      ```ShellSession
      $ aws --version
      ```

   1. Note the output

      *Example:*

      ```
      aws-cli/2.0.33 Python/3.7.3 Linux/3.10.0-1062.12.1.el7.x86_64 botocore/2.0.0dev37
      ```

## Appendix NNN: Manually install Chef Inspec on existing Jenkins Agent

1. Connect to the Jenkins agent

1. Install Chef Inspec

   ```ShellSession
   $ curl https://omnitruck.chef.io/install.sh | sudo bash -s -- -P inspec
   ```

1. Verify Chef Inspec is installed by checking its version

   ```ShellSession
   $ inspec --version
   ```

## Appendix OOO: Connect to Jenkins agent through the Jenkins master using the ProxyJump flag

1. Ensure that you are connected to the Cisco VPN

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Get the public IP address of Jenkins master instance

   ```ShellSession
   $ JENKINS_MASTER_PUBLIC_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PublicIpAddress" \
     --output text)
   ```

1. Get the private IP address of Jenkins agent instance

   ```ShellSession
   $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text)
   ```

1. SSH into the Jenkins agent through the Jenkins master using the ProxyJump flag (-J)

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem -J \
     ec2-user@$JENKINS_MASTER_PUBLIC_IP \
	ec2-user@$JENKINS_AGENT_PRIVATE_IP
   ```

## Appendix PPP: Create and work with CSV database backup

### Create and retrieve CSV database backup

1. Connect to Cisco VPN

1. Set target environment variable

   *Example for "Dev" environment:*

   ```ShellSession
   $ TARGET_ENVIRONMENT=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ TARGET_ENVIRONMENT=ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ TARGET_ENVIRONMENT=ab2d-east-impl
   ```

   *Example for "Prod" environment:*

   ```ShellSession
   $ TARGET_ENVIRONMENT=ab2d-east-prod
   ```

1. Set target environment

   ```ShellSession
   $ source ~/code/ab2d/Deploy/bash/set-env.sh
   ```

1. Get a count of the autoscaling groups in the target environment

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
     --query "AutoScalingGroups[*].AutoScalingGroupName" \
     --output json \
     | jq 'length'
   ```

1. If the number of autscaling groups is not 2, stop and redeploy before proceeding

1. Shut down API nodes

   *Note that adding 75 seconds with "-v+75S" is a Mac-only way of doing this.*

   ```ShellSession
   $ API_AUTOSCALING_GROUP_NAME=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
     --query "AutoScalingGroups[*].AutoScalingGroupName" \
     --output json \
     | jq 'sort' \
     | jq '.[0]' \
     | tr -d '"') \
     && START_TIME=$(date -u -v+75S +%Y-%m-%dT%H:%M:%SZ) \
     && aws --region "${AWS_DEFAULT_REGION}" autoscaling put-scheduled-update-group-action \
     --auto-scaling-group-name "${API_AUTOSCALING_GROUP_NAME}" \
     --scheduled-action-name shutdown-nodes \
     --start-time "${START_TIME}" \
     --min-size 0 \
     --max-size 0 \
     --desired-capacity 0
   ```

1. Shut down worker nodes

   *Note that adding 75 seconds with "-v+75S" is a Mac-only way of doing this.*

   ```ShellSession
   $ WORKER_AUTOSCALING_GROUP_NAME=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
     --query "AutoScalingGroups[*].AutoScalingGroupName" \
     --output json \
     | jq 'sort' \
     | jq '.[1]' \
     | tr -d '"') \
     && START_TIME=$(date -u -v+75S +%Y-%m-%dT%H:%M:%SZ) \
     && aws --region "${AWS_DEFAULT_REGION}" autoscaling put-scheduled-update-group-action \
     --auto-scaling-group-name "${WORKER_AUTOSCALING_GROUP_NAME}" \
     --scheduled-action-name shutdown-nodes \
     --start-time "${START_TIME}" \
     --min-size 0 \
     --max-size 0 \
     --desired-capacity 0
   ```

1. Wait about ten minutes

1. Verify that the API and Worker autoscaling groups have a status of "-" in the AWS console before proceeding
   
1. Backup existing data for target environment

   1. Open Chrome

   1. Open Jenkins

   1. Select target environment folder

   1. Select "devops-engineer-only" folder

   1. Select "02-backup-data-as-csv-for-production"

   1. Select **Build with Parameters**

   1. Select **Build**

   1. Wait for jenkins job to complete

1. Retrieve CSV data from the Jenkins agent

   1. Open a new terminal

   1. Set AWS target environment to management

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Set the Jenkins agent name

      *Example for current Jenkins agent:*

      ```ShellSession
      JENKINS_AGENT_NAME=ab2d-jenkins-agent-old
      ```

      *Example for new in-progress Jenkins agent:*

      ```ShellSession
      JENKINS_AGENT_NAME=ab2d-jenkins-agent
      ```

   1. Get IP address of Jenkins agent

      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=${JENKINS_AGENT_NAME}" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Set target environment variable

      *Example for "Dev" environment:*

      ```ShellSession
      $ TARGET_ENVIRONMENT=ab2d-dev
      ```

      *Example for "Sbx" environment:*
   
      ```ShellSession
      $ TARGET_ENVIRONMENT=ab2d-sbx-sandbox
      ```
   
      *Example for "Impl" environment:*
   
      ```ShellSession
      $ TARGET_ENVIRONMENT=ab2d-east-impl
      ```
   
      *Example for "Prod" environment:*
   
      ```ShellSession
      $ TARGET_ENVIRONMENT=ab2d-east-prod
      ```

   1. Copy the database backup file to the "ec2-user" home directory

      ```ShellSession
      $ ssh -tt -i "~/.ssh/ab2d-mgmt-east-dev.pem" \
        "ec2-user@${JENKINS_AGENT_PRIVATE_IP}" \
        "sudo cp /var/lib/jenkins/database_backup/${TARGET_ENVIRONMENT}.tar.gz /home/ec2-user"
      ```

   1. Change ownership on the database backup file

      ```ShellSession
      $ ssh -tt -i "~/.ssh/ab2d-mgmt-east-dev.pem" \
        "ec2-user@${JENKINS_AGENT_PRIVATE_IP}" \
        "sudo chown ec2-user:ec2-user /home/ec2-user/${TARGET_ENVIRONMENT}.tar.gz"
      ```

   1. Change to the do "Downloads" directory

      ```ShellSession
      $ cd ~/Downloads
      ```

   1. Delete existing target environment directory

      ```ShellSession
      $ rm -rf "${TARGET_ENVIRONMENT}"
      ```
      
   1. Download the database backup file

      ```ShellSession
      $ scp -i ~/.ssh/ab2d-mgmt-east-dev.pem \
        "ec2-user@${JENKINS_AGENT_PRIVATE_IP}:~/${TARGET_ENVIRONMENT}.tar.gz" \
	.
      ```

   1. Uncompress the database backup file

      *Note that "--strip-components=4" must appear at end on command.*

      ```ShellSession
      $ tar -xzvf "${TARGET_ENVIRONMENT}.tar.gz" --strip-components=4
      ```

### Create a second schema that uses CSV database backup

1. Change to the Downloads directory

   *Example for sandbox:*

   ```ShellSession
   $ cd ~/Downloads
   ```

1. Backup the existing database backup

   *Example for sandbox:*

   ```ShellSession
   $ cp -r ~/Downloads/ab2d-sbx-sandbox /tmp/ab2d-sbx-sandbox
   ```

1. Change to the directory where you have your CSV database backup

   *Example for sandbox:*

   ```ShellSession
   $ cd /tmp/ab2d-sbx-sandbox
   ```

1. Copy the public schema file to a backup schema file

   ```ShellSession
   $ cp 01-public-schema.sql 01-backup-schema.sql
   ```

1. Change the schema name to "backup" in the "01-backup-schema.sql" file

   ```ShellSession
   $ sed -i "" 's%public\.%backup\.%g' 01-backup-schema.sql \
     && sed -i "" 's% public % backup %g' 01-backup-schema.sql \
     && sed -i "" 's%public;%backup;%g' 01-backup-schema.sql
   ```

1. Start a database tunnel

1. Open pgAdmin

1. Select the database for the target environment

1. Select the **Tools** menu

1. Select **Query Tool**

1. Execute the following in the "Query Editor"

   ```
   CREATE SCHEMA backup
    AUTHORIZATION cmsadmin;

   GRANT ALL ON SCHEMA backup TO PUBLIC;

   GRANT ALL ON SCHEMA backup TO cmsadmin;
   ```

1. Refesh the target database node in the leftmost panel

1. Select the "Open" icon

1. Select **Yes** to discard changes in the editor

1. Open the following file

   ```
   /tmp/01-backup-schema.sql
   ```

1. Execute the "01-backup-schema.sql" script

1. Close the "01-backup-schema.sql" script

### Reconcile backup data with current data

1. Open a terminal

1. Start a database tunnel (if not already running)

1. Open another terminal

1. Connect to database via a psql shell

1. Import the backed up CSV files to the backup schema by running the following in the psl shell

   *Example for sandbox:*

   ```ShellSession
   \COPY backup.event_api_request FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_api_request_part01.csv' WITH (FORMAT CSV);
   \COPY backup.event_api_request FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_api_request_part02.csv' WITH (FORMAT CSV);
   \COPY backup.event_api_request FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_api_request_part03.csv' WITH (FORMAT CSV);
   \COPY backup.event_api_request FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_api_request_part04.csv' WITH (FORMAT CSV);
   \COPY backup.event_api_request FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_api_request_part05.csv' WITH (FORMAT CSV);
   \COPY backup.event_api_request FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_api_request_part06.csv' WITH (FORMAT CSV);
   \COPY backup.event_api_request FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_api_request_part07.csv' WITH (FORMAT CSV);
   \COPY backup.event_api_request FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_api_request_part08.csv' WITH (FORMAT CSV);
   \COPY backup.event_api_response FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_api_response.csv' WITH (FORMAT CSV);
   \COPY backup.event_bene_reload FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_bene_reload.csv' WITH (FORMAT CSV);
   \COPY backup.event_bene_search FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_bene_search.csv' WITH (FORMAT CSV);
   \COPY backup.event_error FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_error.csv' WITH (FORMAT CSV);
   \COPY backup.event_file FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_file.csv' WITH (FORMAT CSV);
   \COPY backup.event_job_status_change FROM '/tmp/ab2d-sbx-sandbox/csv/public.event_job_status_change.csv' WITH (FORMAT CSV);
   \COPY backup.job FROM '/tmp/ab2d-sbx-sandbox/csv/public.job.csv' WITH (FORMAT CSV);
   \COPY backup.job_output FROM '/tmp/ab2d-sbx-sandbox/csv/public.job_output.csv' WITH (FORMAT CSV);
   ```

1. Open pgAdmin (if not already open)

1. Select the database for the target environment

1. Select the **Tools** menu

1. Select **Query Tool**

1. Import missing records from "event_api_request"

   1. Check current record count of "event_api_request" table in public schema

      ```
      SELECT COUNT(*)
        FROM public.event_api_request;
      ```

   1. Note the record count for the "event_api_request" table in public schema

      *Example:*

      ```
      7955
      ```

   1. Check current record count of "event_api_request" table in backup schema

      ```
      SELECT COUNT(*)
        FROM backup.event_api_request;
      ```

   1. Note the record count for the "event_api_request" table in backup schema

      *Example:*

      ```
      4188
      ```

   1. Get query count of the records to be inserted

      ```
      SELECT COUNT(*)
      FROM backup.event_api_request a
      WHERE a.request_id NOT IN (
        SELECT request_id
        FROM public.event_api_request);
      ```

   1. Verify that the query count equals the "event_api_request" record count in backup schema

      *Example:*

      ```
      4188
      ```

   1. Insert the missing records into the "event_api_request" table

      ```
      INSERT into public.event_api_request(time_of_event, job_id, user_id, url, ip_address, token_hash, request_id, aws_id, environment)
      SELECT time_of_event, job_id, user_id, url, ip_address, token_hash, request_id, aws_id, environment
      FROM backup.event_api_request a
      WHERE a.request_id NOT IN (
        SELECT request_id
        FROM public.event_api_request);
      ```

   1. Re-query the "event_api_request" table

      ```
      SELECT COUNT(*)
        FROM public.event_api_request;
      ```

   1. Verify that the new record count equals the sum of the "event_api_request" original backup and public schemas

      *Example:*

      ```
      12143
      ```

1. Import missing records from "event_api_response"

   1. Check current record count of "event_api_response" table in public schema

      ```
      SELECT COUNT(*)
        FROM public.event_api_response;
      ```

   1. Note the record count for the "event_api_response" table in public schema

      *Example:*

      ```
      6755
      ```

   1. Check current record count of "event_api_response" table in backup schema

      ```
      SELECT COUNT(*)
        FROM backup.event_api_response;
      ```

   1. Note the record count for the "event_api_response" table in backup schema

      *Example:*

      ```
      3116
      ```

   1. Get query count of the records to be inserted

      ```
      SELECT COUNT(*)
      FROM backup.event_api_response a
      WHERE a.request_id NOT IN (
        SELECT request_id
        FROM public.event_api_response);
      ```

   1. Verify that the query count equals the "event_api_response" record count in backup schema

      *Example:*

      ```
      3116
      ```

   1. Insert the missing records into the "event_api_response" table

      ```
      INSERT into public.event_api_response(time_of_event, user_id, job_id, response_code, response_string, description, request_id, aws_id, environment)
      SELECT time_of_event, user_id, job_id, response_code, response_string, description, request_id, aws_id, environment
      FROM backup.event_api_response a
      WHERE a.request_id NOT IN (
        SELECT request_id
        FROM public.event_api_response);
      ```

   1. Re-query the "event_api_response" table

      ```
      SELECT COUNT(*)
        FROM public.event_api_response;
      ```

   1. Verify that the new record count equals the sum of the "event_api_response" original backup and public schemas

      *Example:*

      ```
      9891
      ```

1. Import missing records from "event_bene_reload"

   1. Check current record count of "event_bene_reload" table in public schema

      ```
      SELECT COUNT(*)
        FROM public.event_bene_reload;
      ```

   1. Note the record count for the "event_bene_reload" table in public schema

      *Example:*

      ```
      395
      ```

   1. Check current record count of "event_bene_reload" table in backup schema

      ```
      SELECT COUNT(*)
        FROM backup.event_bene_reload;
      ```

   1. Note the record count for the "event_bene_reload" table in backup schema

      *Example:*

      ```
      91
      ```

   1. Get query count of the records to be inserted

      ```
      SELECT time_of_event, user_id, job_id, file_type, file_name, number_loaded, aws_id, environment
      FROM backup.event_bene_reload a
      WHERE a.time_of_event NOT IN (
        SELECT time_of_event
        FROM public.event_bene_reload);
      ```

   1. Verify that the query count equals the "event_bene_reload" record count in backup schema

      *Example:*

      ```
      91
      ```

   1. Insert the missing records into the "event_bene_reload" table

      ```
      INSERT into public.event_bene_reload(time_of_event, user_id, job_id, file_type, file_name, number_loaded, aws_id, environment)
      SELECT time_of_event, user_id, job_id, file_type, file_name, number_loaded, aws_id, environment
      FROM backup.event_bene_reload a
      WHERE a.time_of_event NOT IN (
        SELECT time_of_event
        FROM public.event_bene_reload);
      ```

   1. Re-query the "event_bene_reload" table

      ```
      SELECT COUNT(*)
        FROM public.event_bene_reload;
      ```

   1. Verify that the new record count equals the sum of the "event_bene_reload" original backup and public schemas

      *Example:*

      ```
      486
      ```

1. Import missing records from "event_bene_search"

   1. Check current record count of "event_bene_search" table in public schema

      ```
      SELECT COUNT(*)
        FROM public.event_bene_search;
      ```

   1. Note the record count for the "event_bene_search" table in public schema

      *Example:*

      ```
      72
      ```

   1. Check current record count of "event_bene_search" table in backup schema

      ```
      SELECT COUNT(*)
        FROM backup.event_bene_search;
      ```

   1. Note the record count for the "event_bene_search" table in backup schema

      *Example:*

      ```
      87
      ```

   1. Get query count of the records to be inserted

      ```
      SELECT COUNT(*)
      FROM backup.event_bene_search a
      WHERE a.time_of_event NOT IN (
        SELECT time_of_event
        FROM public.event_bene_search);
      ```

   1. Verify that the query count equals the "event_bene_search" record count in backup schema

      *Example:*

      ```
      87
      ```

   1. Insert the missing records into the "event_bene_search" table

      ```
      INSERT into public.event_bene_search(time_of_event, user_id, job_id, contract_number, num_in_contract, num_searched, num_opted_out, num_errors, aws_id, environment)
      SELECT time_of_event, user_id, job_id, contract_number, num_in_contract, num_searched, num_opted_out, num_errors, aws_id, environment
      FROM backup.event_bene_search a
      WHERE a.time_of_event NOT IN (
        SELECT time_of_event
        FROM public.event_bene_search);
      ```

   1. Re-query the "event_bene_search" table

      ```
      SELECT COUNT(*)
        FROM public.event_bene_search;
      ```

   1. Verify that the new record count equals the sum of the "event_bene_search" original backup and public schemas

      *Example:*

      ```
      159
      ```

1. Import missing records from "event_error"

   1. Check current record count of "event_error" table in public schema

      ```
      SELECT COUNT(*)
        FROM public.event_error;
      ```

   1. Note the record count for the "event_error" table in public schema

      *Example:*

      ```
      19
      ```

   1. Check current record count of "event_error" table in backup schema

      ```
      SELECT COUNT(*)
        FROM backup.event_error;
      ```

   1. Note the record count for the "event_error" table in backup schema

      *Example:*

      ```
      11
      ```

   1. Get query count of the records to be inserted

      ```
      SELECT COUNT(*)
      FROM backup.event_error a
      WHERE a.time_of_event NOT IN (
        SELECT time_of_event
        FROM public.event_error);
      ```

   1. Verify that the query count equals the "event_error" record count in backup schema

      *Example:*

      ```
      11
      ```

   1. Insert the missing records into the "event_error" table

      ```
      INSERT into public.event_error(time_of_event, user_id, job_id, error_type, description, aws_id, environment)
      SELECT time_of_event, user_id, job_id, error_type, description, aws_id, environment
      FROM backup.event_error a
      WHERE a.time_of_event NOT IN (
        SELECT time_of_event
        FROM public.event_error);
      ```

   1. Re-query the "event_error" table

      ```
      SELECT COUNT(*)
        FROM public.event_error;
      ```

   1. Verify that the new record count equals the sum of the "event_error" original backup and public schemas

      *Example:*

      ```
      30
      ```

1. Import missing records from "event_file"

   1. Check current record count of "event_file" table in public schema

      ```
      SELECT COUNT(*)
        FROM public.event_file;
      ```

   1. Note the record count for the "event_file" table in public schema

      *Example:*

      ```
      222
      ```

   1. Check current record count of "event_file" table in backup schema

      ```
      SELECT COUNT(*)
        FROM backup.event_file;
      ```

   1. Note the record count for the "event_file" table in backup schema

      *Example:*

      ```
      276
      ```

   1. Get query count of the records to be inserted

      ```
      SELECT COUNT(*)
      FROM backup.event_file a
      WHERE a.time_of_event NOT IN (
        SELECT time_of_event
        FROM public.event_file);
      ```

   1. Verify that the query count equals the "event_file" record count in backup schema

      *Example:*

      ```
      276
      ```

   1. Insert the missing records into the "event_file" table

      ```
      INSERT into public.event_file(time_of_event, user_id, job_id, file_name, status, file_size, file_hash, aws_id, environment)
      SELECT time_of_event, user_id, job_id, file_name, status, file_size, file_hash, aws_id, environment
      FROM backup.event_file a
      WHERE a.time_of_event NOT IN (
        SELECT time_of_event
        FROM public.event_file);
      ```

   1. Re-query the "event_file" table

      ```
      SELECT COUNT(*)
        FROM public.event_file;
      ```

   1. Verify that the new record count equals the sum of the "event_file" original backup and public schemas

      *Example:*

      ```
      498
      ```

1. Import missing records from "event_job_status_change"

   1. Check current record count of "event_job_status_change" table in public schema

      ```
      SELECT COUNT(*)
        FROM public.event_job_status_change;
      ```

   1. Note the record count for the "event_job_status_change" table in public schema

      *Example:*

      ```
      229
      ```

   1. Check current record count of "event_job_status_change" table in backup schema

      ```
      SELECT COUNT(*)
        FROM backup.event_job_status_change;
      ```

   1. Note the record count for the "event_job_status_change" table in backup schema

      *Example:*

      ```
      278
      ```

   1. Get query count of the records to be inserted

      ```
      SELECT COUNT(*)
      FROM backup.event_job_status_change a
      WHERE a.time_of_event NOT IN (
        SELECT time_of_event
        FROM public.event_job_status_change);
      ```

   1. Verify that the query count equals the "event_job_status_change" record count in backup schema

      *Example:*

      ```
      278
      ```

   1. Insert the missing records into the "event_job_status_change" table

      ```
      INSERT into public.event_job_status_change(time_of_event, user_id, job_id, old_status, new_status, description, aws_id, environment)
      SELECT time_of_event, user_id, job_id, old_status, new_status, description, aws_id, environment
      FROM backup.event_job_status_change a
      WHERE a.time_of_event NOT IN (
        SELECT time_of_event
        FROM public.event_job_status_change);
      ```

   1. Re-query the "event_job_status_change" table

      ```
      SELECT COUNT(*)
        FROM public.event_job_status_change;
      ```

   1. Verify that the new record count equals the sum of the "event_job_status_change" original backup and public schemas

      *Example:*

      ```
      507
      ```

1. Import missing records from "job"

   1. Check current record count of "job" table in public schema

      ```
      SELECT COUNT(*)
        FROM public.job;
      ```

   1. Note the record count for the "job" table in public schema

      *Example:*

      ```
      76
      ```

   1. Check current record count of "job" table in backup schema

      ```
      SELECT COUNT(*)
        FROM backup.job;
      ```

   1. Note the record count for the "job" table in backup schema

      *Example:*

      ```
      92
      ```

   1. Get query count of the records to be inserted

      ```
      SELECT COUNT(*)
      FROM backup.job a
      WHERE a.job_uuid NOT IN (
        SELECT job_uuid
        FROM public.job);
      ```

   1. Verify that the query count equals the "job" record count in backup schema

      *Example:*

      ```
      92
      ```

   1. Insert the missing records into the "job" table

      ```
      INSERT into public.job(id, job_uuid, user_account_id, created_at, expires_at, resource_types, status, status_message, request_url, progress, last_poll_time, completed_at, contract_id, output_format, since)
      SELECT nextval('hibernate_sequence'), job_uuid, user_account_id, created_at, expires_at, resource_types, status, status_message, request_url, progress, last_poll_time, completed_at, contract_id, output_format, since
      FROM backup.job a
      WHERE a.job_uuid NOT IN (
        SELECT job_uuid
        FROM public.job);
      ```

   1. Re-query the "job" table

      ```
      SELECT COUNT(*)
        FROM public.job;
      ```

   1. Verify that the new record count equals the sum of the "job" original backup and public schemas

      *Example:*

      ```
      168
      ```

1. Import missing records from "job_output"

   1. Check current record count of "job_output" table in public schema

      ```
      SELECT COUNT(*)
        FROM public.job_output;
      ```

   1. Note the record count for the "job_output" table in public schema

      *Example:*

      ```
      72
      ```

   1. Check current record count of "job_output" table in backup schema

      ```
      SELECT COUNT(*)
        FROM backup.job_output;
      ```

   1. Note the record count for the "job_output" table in backup schema

      *Example:*

      ```
      88
      ```

   1. Get query count of the records to be inserted

      ```
      SELECT COUNT(*)
      FROM public.job a
      LEFT OUTER JOIN backup.job b
      ON a.job_uuid = b.job_uuid
      INNER JOIN backup.job_output c
      ON b.id = c.job_id
      WHERE a.job_uuid IN (
        SELECT job_uuid
        FROM backup.job);
      ```

   1. Verify that the query count equals the "job_output" record count in backup schema

      *Example:*

      ```
      88
      ```

   1. Insert the missing records into the "job_output" table

      ```
      INSERT into public.job_output(id, job_id, file_path, fhir_resource_type, error, downloaded, checksum, file_length)
      SELECT nextval('hibernate_sequence'), a.id AS job_id, c.file_path, c.fhir_resource_type, c.error, c.downloaded, c.checksum, c.file_length
      FROM public.job a
      LEFT OUTER JOIN backup.job b
      ON a.job_uuid = b.job_uuid
      INNER JOIN backup.job_output c
      ON b.id = c.job_id
      WHERE a.job_uuid IN (
        SELECT job_uuid
        FROM backup.job);
      ```

   1. Re-query the "job_output" table

      ```
      SELECT COUNT(*)
        FROM public.job_output;
      ```

   1. Verify that the new record count equals the sum of the "job_output" original backup and public schemas

      *Example:*

      ```
      160
      ```

## Appendix QQQ: Get private IP address

1. Get private IP address on Mac

   ```ShellSession
   $ ipconfig getifaddr en0
   ```

1. Note the private address that is output

## Appendix RRR: Protect the existing RDS database using AWS CLI

1. Change to the "bash" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/bash
   ```

1. Set target environment

   ```ShellSession
   $ source ./set-env.sh
   ```

1. Apply delete protection to the existing RDS instance

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" rds modify-db-instance \
     --db-instance-identifier 'ab2d' \
	--deletion-protection
   ```

1. Verify that delete protection is true

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" rds describe-db-instances \
     --query "DBInstances[?DBInstanceIdentifier=='ab2d'].DeletionProtection" \
	--output text
   ```

## Appendix SSS: Review RDS reserved instance utilization from AWS console

### Create a "RDS Reserved Instance Utilization" report

1. Log on to AWS console for the target environment

1. Select **Cost Explorer**

1. Select **Utilization report** under "Reservations" in the leftmost panel

1. Select **Service** under "Filters" in the rightmost panel

1. Select the **Relational Database Service (RDS)** radio button

1. Select **Apply Filters**

1. Note the solid blue line starting on June 19th that shows 100% utilization of the reserved RDS instance

1. Select **Save as**

1. Type the following in the "Save as new report" text box

   ```
   RDS Reserved Instance Utilization
   ```

1. Select **Save Report**

### Run the "RDS Reserved Instance Utilization" report

1. Log on to AWS console for the target environment

1. Select **Cost Explorer**

1. Select **Reports** in the leftmost panel

1. Enter the following in the "Search" text box

   ```
   rds
   ```

1. Select the following report

   ```
   RDS Reserved Instance Utilization
   ```

## Appendix TTT: Reset master to a specific commit

### Force push to master

1. Temporarily configure master branch to allow force push

   1. Note that you need to be an administrator of the repo to do this

   1. Open Chrome

   1. Enter the following in the address bar

      > https://github.com/CMSgov/ab2d

   1. Select **Settings**

   1. Select **Branches** from the leftmost panel

   1. Select **Edit** beside "master" under the "Branch protection rules" section

   1. Uncheck **Require pull request reviews before merging**

   1. Uncheck **Require status checks to pass before merging**

   1. Check **Allow force pushes**

   1. Select **Save changes**

1. Be sure to stash or check-in any changes that you have in your current branch

1. Update your local origin branches

   ```ShellSession
   $ git fetch --all
   ```

1. Checkout the master branch

   ```ShellSession
   $ git checkout master
   ```

1. Do a hard reset to desired commit number

   *Format:*

   ```ShellSession
   $ git reset --hard {commit number}
   ```

1. Force push to master

   ```ShellSession
   $ git push --force
   ```

1. Re-configure master branch to original settings

   1. Note that you need to be an administrator of the repo to do this

   1. Open Chrome

   1. Enter the following in the address bar

      > https://github.com/CMSgov/ab2d

   1. Select **Settings**

   1. Select **Branches** from the leftmost panel

   1. Select **Edit** beside "master" under the "Branch protection rules" section

   1. Check **Require pull request reviews before merging**

   1. Select "2" from the **Required approving reviews** dropdown

   1. Check **Dismiss stale pull request approvals when new commits are pushed**

   1. Check **Require status checks to pass before merging**

   1. Check **Require branches to be up to date before merging**

   1. Check **LGTM analysis: Java**

   1. Check **continuous-integration/travis-ci**

   1. Uncheck **Allow force pushes**

   1. Select **Save changes**

### Reconcile your master branch with a remote branch that was force pushed by someone else

1. If you are currently on the master branch, checkout a different branch

   *Format:*
   
   ```ShellSession
   $ git checkout {a branch other than master}
   ```

1. Delete your local working master branch

   ```ShellSession
   $ git branch -D master
   ```

1. Update your local "origin/master" branch

   ```ShellSession
   $ git fetch --all
   ```

1. Checkout your new local working master branch

   ```ShellSession
   $ git checkout master
   ```

1. Return to the branch that you are working on

   ```ShellSession
   $ git checkout {a branch other than master}
   ```
   
### Rebase an existing branch to reconcile it with a master that has been reset

1. Get the number of commits that have been committed to your branch after the current master commit

   ```ShellSession
   $ COMMIT_NUMBER_OF_ORIGIN_MASTER=$(git rev-parse origin/master | cut -c1-8) \
     && NUMBER_OF_COMMITS_AFTER_MASTER=$(git log --oneline \
     | awk '{ print $1 }' \
     | awk "/${COMMIT_NUMBER_OF_ORIGIN_MASTER}/ {exit} {print}" \
     | wc -l \
     | tr -d ' ')
   ```

2. Rebase your branch

   ```ShellSession
   $ git rebase -i "HEAD~${NUMBER_OF_COMMITS_AFTER_MASTER}"
   ```

3. Note that vim opens with the list of commits that include the most recent commits to your branch (some additional commits might show up, but that is normal)

4. Change the commits that are not yours by changing "pick" to "drop"

   Notes:

   - only do the most recent commits that occurred after the master commit

   - identify the commits that are not yours by looking at your branch in GitHub

5. Save and close the vim editor

6. If the rebase is successful, force push the changes to your branch

   ```ShellSession
   $ git push --force-with-lease
   ```

7. Update your local "origin/master"

   ```ShellSession
   $ git fetch --all
   ```

8. Merge from your local "origin/master"

   ```ShellSession
   $ git merge origin/master
   ```

9. Push the changes

   ```ShellSession
   $ git push
   ```

## Appendix UUU: Migrate VictorOps-Slack integration from a real slack user to slack service user

1. Create an "sb victorops admin slack" slack service user for the SemanticBits Slack workspace

   *A SemanticBits Slack workspace administrator must do this step.*

1. Add the slack service user to the "p-ab2d-incident-response" channel

1. Add the slack service user to the "p-ccxp-alerts" channel

   *A CCXP user must do this step.*

1. Log out of SemanticBits slack workspace

1. Log on to SemanticBits slack workspace as the slack service user

1. Log on to VictorOps

1. Select the **Integrations** tab

1. Type "slack" in the **Search** text box

1. Select **Slack**

1. Select **Revoke Integration**

1. Re-enable the slack integration using the "sb victorops admin slack" slack user

   1. Select **Enable Integration**

   1. Select **Allow**

   1. Select the following from the **Select a channel to send VictorOps messages to** dropdown

      ```
      p-ccxp-alerts
      ```

   1. Check **Chat Messages (Synced with VictorOps Timeline)**

   1. Check **On-Call change notifications**

   1. Check **Paging notifications**

   1. Check **Incidents**

   1. Select **Save** in the "Default Channel" page

   1. Select **OK** on the "Success" dialog

   1. Select **Add Mapping**

   1. Select "AB2D - Standard" from the **Select an Escalation Policy** dropdown

   1. Select the following from the **Select a channel to send VictorOps messages to** dropdown

      ```
      p-ab2d-incident-response
      ```

   1. Check **Chat Messages (Synced with VictorOps Timeline)**

   1. Check **On-Call change notifications**

   1. Check **Paging notifications**

   1. Check **Incidents**

   1. Select **Save** in the "Default Channel" page

1. Log out of slack service user

1. Log on with your real user

1. Test using SNS/CloudWatch

   1. Temporarily make yourself on-call in VictorOps for the next available 30 minute block
   
   1. Open a new Chrome tab
   
   1. Log on to the AWS account
   
   1. Select **Simple Notification Service**
   
   1. Select **Topics** in the leftmost panel
   
   1. Select the following
   
      ```
      ab2d-east-prod-cloudwatch-alarms
      ```
   
   1. Select the **Subscriptions** tab
   
   1. Select **Create subscription**
   
   1. Configure the "Create subscription" page as follows
   
      - **Topic ARN:** {keep default}
   
      - **Protocol:** HTTPS
   
      - **Endpoint:** {victors ops service api endpoint for aws cloudwatch}/{routing key}
   
      - **Enable raw message delivery:** unchecked
   
   1. Select **Create subscription**
   
   1. Wait for "Status" to display the following
   
      *Note that you will likely need to refesh the page to see the status change to "Confirmed".*
   
      ```
      Confirmed
      ```
   
   1. Select **Topics** from the leftmost panel
   
   1. Select the following topic
   
      ```
      ab2d-east-prod-cloudwatch-alarms
      ```
   
   1. Select **Publish message**
   
   1. Configure the "Message details" section as follows
   
      - **Subject:** {keep blank}
   
      - **Time to Live (TTL):** {keep blank}
   
   1. Configure the "Message body" section as follows
   
      - **Message structure:** Identical payload for all delivery protocols
   
      - **Message body to send to the endpoint:**
   
        ```
        {"AlarmName":"AB2D Prod - VictorOps - CloudWatch Integration TEST","NewStateValue":"ALARM","NewStateReason":"failure","StateChangeTime":"2017-12-14T01:00:00.000Z","AlarmDescription":"VictorOps - CloudWatch Integration TEST"}
        ```
   
   1. Select **Publish message**
   
   1. Verify that the message is received in VictorOps

   1. Acknowledge the incident via cell phone, slack, or VictorOps

   1. Resolve the incident via cell phone, slack, or VictorOps

## Appendix VVV: Add a volume to jenkins agent and extend the root volume to use it

1. Note that these directions assume that you already have two volumes and want to add a third volume

1. Create a new volume in the same availabilty zone as Jenkins agent in the AWS console

1. Attach the volume to the Jenkins agent in the AWS console

1. Connect to the Jenkins agent

1. Set the partition as gpt

   ```ShellSession
   $ sudo parted --script /dev/nvme2n1 mklabel gpt
   ```

1. View detail about the disks and partitions again

   1. Enter the following

      ```ShellSession
      $ sudo parted -l
      ```

   1. Note the output

      ```
      Model: NVMe Device (nvme)
      Disk /dev/nvme0n1: 805GB
      Sector size (logical/physical): 512B/512B
      Partition Table: msdos
      Disk Flags:

      Number  Start   End     Size    Type     File system  Flags
       1      1049kB  1075MB  1074MB  primary  xfs
       2      1075MB  32.2GB  31.1GB  primary               lvm
       3      32.2GB  268GB   236GB   primary
       4      268GB   537GB   268GB   primary


      Model: NVMe Device (nvme)
      Disk /dev/nvme1n1: 537GB
      Sector size (logical/physical): 512B/512B
      Partition Table: gpt
      Disk Flags:

      Number  Start   End    Size   File system  Name  Flags
       1      1049kB  537GB  537GB


      Model: NVMe Device (nvme)
      Disk /dev/nvme2n1: 537GB
      Sector size (logical/physical): 512B/512B
      Partition Table: gpt
      Disk Flags:

      Number  Start  End  Size  File system  Name  Flags
      ```

1. Create a new partition on the "/dev/nvme2n1" disk

   ```ShellSession
   (
   echo n # Add a new partition
   echo p # Primary partition
   echo   # Partition number (Accept default)
   echo   # First sector (Accept default)
   echo   # Last sector (Accept default)
   echo w # Write changes
   ) | sudo fdisk /dev/nvme2n1
   ```

1. Request that the operating system re-reads the partition table

   ```ShellSession
   $ sudo partprobe
   ```

1. Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

   ```ShellSession
   $ sudo pvcreate /dev/nvme2n1p1
   ```

1. Format the "/dev/nvme2n1" disk as xfs

   ```ShellSession
   $ sudo mkfs.xfs -f /dev/nvme2n1p1
   ```

1. Extend the "VolGroup00" volume group to include the new volume

   ```ShellSession
   $ sudo vgextend VolGroup00 /dev/nvme2n1p1
   ```

1. Extend the size of the "log" logical volume with all the free space on the new volume

   ```ShellSession
   $ sudo lvextend -l +100%FREE /dev/mapper/VolGroup00-rootVol
   ```

1. Expands the existing XFS filesystem

   ```ShellSession
   $ sudo xfs_growfs -d /dev/mapper/VolGroup00-rootVol
   ```

## Appendix WWW: Whitelist IP addresses in Akamai for Prod

1. Log on to Akamai

1. Select the three bar icon in the top left of the page

1. Scroll down to the "WEB & DATA CENTER SECURITY" section

1. Expand the **Security Configurations** node

1. Select **Network Lists**

1. Note that there are currently four whitelists associated with AB2D Prod

   - AB2D_PROD_VPN_WHITELIST <-- lists CMS VPN ip addresses

   - AB2D_PROD_NEWRELIC_WHITELIST <-- not currently used

   - AB2D_PROD_SHAREDSERVICES_WHITELIST <-- not currently used

   - AB2D_PROD_ACO_WHITLEIST <-- lists Accountable Care Organization (ACO) ip addresses
   
1. Expand the **AB2D_PROD_ACO_WHITLEIST** node

1. Type or copy and IP address into the **Add Items** text box

1. Tab away from the text box

1. Note that the IP address now appears in the **Items in the list** text box

1. Select **Save Changes**

1. Collapse the **AB2D_PROD_ACO_WHITLEIST** node

1. Select **...** beside the "AB2D_PROD_ACO_WHITLEIST" node

1. Select **Activate** from the context menu

1. Select the **Production** radio button

1. Select **Activate List**

## Appendix XXX: Fix CloudTamer scripts broken by ITOPS role change

1. Ensure that you are on CMS VPN

1. Open Chrome

1. Log on to CloudTamer

1. Select your logon dropdown near the upper right of the page

1. Note the value under "Federated Login"

   *Format:*

   ```
   {role}/{your eua id}
   ```

1. At the time of writing, the role is the following:

   ```
   ct-ado-ab2d-application-admin
   ```

1. If ITOPS automation has changed the name of this role, you must update the role name in two places

   - within the previous step of this documentation

   - within the following scripts

     - ./Deploy/bash/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh

     - ./Deploy/terraform/modules/kms/main.tf

## Appendix YYY: Add IAM components under the new ITOPS restrictions

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set the target environment

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Create a test policy

   ```ShellSession
   $ aws --region $AWS_DEFAULT_REGION iam create-policy \
     --policy-name Ab2dTestPolicy \
     --path "/delegatedadmin/developer/" \
     --policy-document "file://Deploy/test-files/ab2d-cloudtrail-cloudwatch-policy.json"
   ```

1. Get policy ARN of test policy

   ```ShellSession
   $ AB2D_TEST_POLICY_ARN=$(aws --region us-east-1 iam list-policies \
     --query 'Policies[?PolicyName==`Ab2dTestPolicy`].{ARN:Arn}' \
     --output text)
   ```

## Appendix ZZZ: Revert your branch to a previous commit and force push to GitHub

1. Ensure you stash or commit any changes in your branch

1. Do a hard reset to the desired commit number

   *Format:*

   ```ShellSession
   $ git reset --hard {desired commit number}
   ```

1. Do a force push to GitHub

   ```ShellSession
   $ git push --force
   ```

## Appendix AAAA: Change encryption key of AWS RDS Instance

### Create manual snapshot of RDS DB instance

1. Log on to the target AWS environment

1. Select **RDS**

1. Select **Databases** from the leftmost panel

1. Select the radio button beside the desired DB instance

1. Select the **Actions** dropdown

1. Select **Take snapshot**

1. Type the following in the **Snapshot name** text box

   *Example:*
   
   ```
   ab2d-db-snapshot-2020-12-22-1730
   ```

1. Select **Take snapshot**

1. Scroll to the right in the "Manual snapshots" table and note the **Progress** column

1. Wait for **Progress** to display "Completed" for the snapshot

   *Note that you will need to select the refresh icon to get an update on the status.*

### Copy manual snapshot using desired KMS key

1. Log on to the target AWS environment

1. Select **RDS**

1. Select **Snapshots** from the leftmost panel

1. Select the **Manual** tab

1. Select the checkbox beside the desired snapshot

   *Example:*
   
   ```
   ab2d-db-snapshot-2020-12-22-1730
   ```

1. Select the **Actions** dropdown

1. Select **Copy snapshot**

1. Configure the "Settings" section as follows

   *Example:*
   
   - **Destination Region:** US East (N. Virginia)

   - **New DB Snapshot Identifier:** ab2d-db-snapshot-with-ab2d-kms-2020-12-22-1730

   - **Copy Tags:** checked

1. Configure the "Encryption" section as follows

   - **Master key:** ab2d-kms

1. Select **Copy snapshot**

1. Scroll to the right in the "Manual snapshots" table and note the **Progress** column

1. Wait for **Progress** to display "Completed" for the snapshot

   *Note the following:*

   - select the refresh icon to get an update on the status

   - depending on the amount of data, this process may take hours

### Create new RDS DB instance from the snapshot copy

1. Log on to the target AWS environment

1. Select **RDS**

1. Select **Snapshots** from the leftmost panel

1. Select the **Manual** tab

1. Select the checkbox beside the desired snapshot

   *Example:*
   
   ```
   ab2d-db-snapshot-with-ab2d-kms-2020-12-22-1730
   ```

1. Select the **Actions** dropdown

1. Select **Restore snapshot**

1. Configure the "DB specifications" section as follows
   
   - **Engine:** PostgreSQL

1. Configure the "Settings" section as follows

   *Example:*
   
   - **DB Snapshot ID:** ab2d-db-snapshot-with-ab2d-kms-2020-12-22-1730

   - **DB Instance identifier:** ab2d-new

1. Configure the "Connectivity" section as follows

   - **Virtual private cloud (VPC):** ab2d-dev

   - **Subnet group:** ab2d-rds-subnet-group

   - **Public access:** No

   - **VPC security group:** Choose existing

   - **Existing VPC security groups:** ab2d-database-sg (make sure to delete "default")

   - **Additional configuration - Database port:** 5432

1. Configure the "DB instance size" section as follows

   - **DB instance class radio button:** Standard classes (includes m classes)

   - **DB instance class:** db.m4.2xlarge (note that 'Include previous generation classes' must be selected first in order to choose this instance class)

   - **Include previous generation classes:** selected

1. Configure the "Storage" section as follows

   - **Storage type:** Provisioned IOPS (SSD)

   - **Allocated storage:** 500

   - **Provisioned IOPS:** 5000

1. Configure the "Availability & durability" section as follows

   *Example for Prod or Sbx:*

   - multi-az

   *Example for Dev or Impl:*

   - single az

1. Configure the "Database authentication" section as follows

   - Password authentication

1. Note that the "Encryption" section was configued when the snapshot was created

1. Configure the "Additional configuration" section as follows

   **DB parameter group:** ab2d-rds-parameter-group
   
   **Option group:** default:postgres-11

   **Copy tags to snapshots:** checked

   **Postgresql log:** checked

   **Upgrade log:** checked

   **Enable auto minor version upgrade:** checked

1. Select **Restore DB Instance**

### Configure deployment to use new DB instance

- rename old instance to ab2d-old

- rename new instance to ab2d

- do I need to do anything with database host in secrets manager?

- do I need to do anything with terraform state?
