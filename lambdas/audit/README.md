On a configurable interval, cloudwatch triggers this lambda. As of writing that interval is every 2 hours. 

- Open the ab2d efs mount
- Scan the filesystem for  
  - Folders with a UUID as its name
  - Files that end with .ndjosn
- If files are older that the time to live (72 hours currently) 
  - Delete any files older than ttl
- Delete empty folders

## Build/Deploy/etc

Please see the [README.md](../README.md) in the root directory of this project.

