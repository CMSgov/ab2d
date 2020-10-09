# Bash Client

```
Usage: 
  source ./bootstrap.sh (-prod | -sandbox) --auth <base64 username:password> [--contract <contract number>] [--directory <dir>]
  ./run-job.sh (-prod | -sandbox) --auth <base64 username:password> [--contract <contract number>] [--directory <dir>]
  ./start-job.sh
  ./monitor-job.sh
  ./download-results.sh
Arguments:
  -sandbox -- if running against ab2d sandbox environment
  -prod -- if running against ab2d production environment
  --auth -- base64 encoded "clientid:password"
  --contract -- if searching specific contract then give contract number ex. Z0001
  --directory -- if you want files and job info saved to specific directory
```

Limitations:

1. Assumes all scripts use the same directory

2. Assumes all scripts use the same base64 encoded AUTH token

Example:

*If you want to:*

1. Start a job running against production

2. Pull a specific contract named 'ABCDE'

3. Save all results for this job to the directory /opt/foo

*Run the following command:*

```bash
source ./bootstrap.sh -prod --auth $AUTH --contract ABCDE --directory /opt/foo && ./start-job.sh
```

## Scripts Included

1. start-job.sh: start a job given an auth token, contract, and environment

2. monitor-job.sh: monitor a running job until it completes

3. download-results.sh: download results from a job that has been run

4. run-job.sh: aggregation of the first three scripts

The last script combines the first three steps into one script.

### Other resources included

1. fn_get_token: take a base64 encoded secret and retrieve a JWT token

2. bootstrap.sh: take command line arguments and export variables necessary for other scripts
(called individually in start-job.sh, monitor-job.sh, and download-results.sh)

## Extended Example Instructions

For this example the job is being run against sandbox

### Running Scripts Individually

1. Set the OKTA_CLIENT_ID and OKTA_CLIENT_PASSWORD

   ```bash
   OKTA_CLIENT_ID=<client id>
   OKTA_CLIENT_PASSWORD=<client password>
   ```

1. Create the AUTH token `AUTH=$(echo -n "${OKTA_CLIENT_ID}:${OKTA_CLIENT_PASSWORD}" | base64)`

1. Run `source ./bootstrap.sh -sandbox --auth $AUTH` to set environment variables for a job.

1. Run `./start-job.sh` to start a job. If successful a file containing
the job id will be saved in `<directory>/jobId.txt`

1. Run `./monitor-job.sh` which will monitor the state of the running job. When the job
finished the full HTTP response will be saved to `<directory>/response.json`

1. Run `./download-results.sh`

### Running Aggregate Script

1. Set the OKTA_CLIENT_ID and OKTA_CLIENT_PASSWORD

   ```bash
   OKTA_CLIENT_ID=<client id>
   OKTA_CLIENT_PASSWORD=<client password>
   ```
   
1. Create the AUTH token `AUTH=$(echo -n "${OKTA_CLIENT_ID}:${OKTA_CLIENT_PASSWORD}" | base64)`

1. Run `./run-job.sh -sandbox --auth $AUTH` to start, monitor, and download results from a job.
