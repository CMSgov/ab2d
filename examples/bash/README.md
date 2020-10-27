# Bash Client

A simple client for starting a job in sandbox or production, monitor that job,
and download the results. To prevent issues these scripts persist the job
id and list of files generated.

This script will not overwrite already existing export files.

```
Usage: 
  bootstrap (-prod | -sandbox) --auth <base64 username:password> [--contract <contract number>] [--directory <dir>]
  run-job (-prod | -sandbox) --auth <base64 username:password> [--contract <contract number>] [--directory <dir>]
  start-job
  monitor-job
  download-results

Arguments:
  -sandbox -- if running against ab2d sandbox environment
  -prod -- if running against ab2d production environment
  --auth -- base64 encoded "clientid:password"
  --contract -- if searching specific contract then give contract number ex. Z0001
  --directory -- if you want files and job info saved to specific directory
```

Files:

1. /directory/jobId.txt -- id of the job created
2. /directory/response.json -- list of files created 
3. /directory/*.ndjson -- downloaded results of exports 

Limitations:

1. Assumes all scripts use the same directory
2. Assumes all scripts use the same base64 encoded AUTH token

Example:

If you want to:
1. Start a job running against production
2. Pull a specific contract named 'ABCDE'
3. And save all results for this job to the directory /opt/foo

Then run the following command
`./bootstrap.sh -prod --auth $AUTH --contract ABCDE --directory /opt/foo &&
 ./start-job.sh && ./monitor-job.sh && ./download-results.sh`


## Scripts Included

1. bootstrap.sh: prepare environment variables necessary for other scripts using command line arguments
1. start-job.sh: start a job given an auth token, contract, and environment
1. monitor-job.sh: monitor a running job until it completes
1. download-results.sh: download results from a job that has been run
1. run-job.sh: aggregation of the first three scripts

The last script combines the first four steps into one script.

### Other resources included

1. fn_get_token.sh: take a base64 encoded secret and retrieve a JWT token

## Extended Example Instructions

For this example the job is run against sandbox.

### Running Scripts Individually

1. Set the OKTA_CLIENT_ID and OKTA_CLIENT_PASSWORD
   ```bash
   OKTA_CLIENT_ID=<client id>
   OKTA_CLIENT_PASSWORD=<client password>
   ```
1. Create the AUTH token `AUTH=$(echo -n "${OKTA_CLIENT_ID}:${OKTA_CLIENT_PASSWORD}" | base64)`
1. Run `source bootstrap.sh -sandbox --auth $AUTH` to set environment variables for a job.
1. Run `./start-job.sh` to start a job. If successful a file containing
the job id will be saved in `<directory>/jobId.txt`
1. Run `./monitor-job.sh` which will monitor the state of the running job. When the job
finished the full HTTP response will be saved to `<directory>/response.json`
1. Run `./download-results.sh` to get the files. This will only download the files once. Running again
will not overwrite the files but will also not download anything.

### Running Aggregate Script
1. Set the OKTA_CLIENT_ID and OKTA_CLIENT_PASSWORD
   ```bash
   OKTA_CLIENT_ID=<client id>
   OKTA_CLIENT_PASSWORD=<client password>
   ```
2. Create the AUTH token `AUTH=$(echo -n "${OKTA_CLIENT_ID}:${OKTA_CLIENT_PASSWORD}" | base64)`
3. Run `./run-job.sh -sandbox --auth $AUTH` to start, monitor, and download results from a job.
