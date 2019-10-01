# TO DO

## Table of Contents

1. [To do after granted access to AWS account](#to-do-after-granted-access-to-aws-account)

## To do after granted access to AWS account

1. Verify the gold AMI

   ```
   ami-01962ba1fa3692903
   ```
   
1. Open "app.json" for "app" under packer

   ```ShellSession
   $ vim ./Deploy/packer/app/app.json
   ```

1. Update the following

   ```
   "gold_ami": "ami-01962ba1fa3692903"
   "subnet_id": "SUBNET_ID",
   "vpc_id": "VPC_ID",
   ```

