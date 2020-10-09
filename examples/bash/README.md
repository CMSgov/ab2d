# Bash Client

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

Limitations:

1. Assumes all scripts use the same directory
2. Assumes all scripts use the same base64 encoded AUTH token

Example:

If you want to:
1. Start a job running against production
2. Pull a specific contract named 'ABCDE'
3. And save all results for this job to the directory /opt/foo

Then run the following command
`./bootstrap -prod --auth $AUTH --contract ABCDE --directory /opt/foo &&
 ./start-job`

## Scripts Included

1. start-job: start a job given an auth token, contract, and environment
2. monitor-job: monitor a running job until it completes
3. download-results: download results from a job that has been run
4. run-job: aggregation of the first three scripts

The last script combines the first three steps into one script.

### Other resources included

1. fn_get_token: take a base64 encoded secret and retrieve a JWT token
2. bootstrap: take command line arguments and export variables necessary for other scripts
(called individually in start-job, monitor-job, and download-results)

## Extended Example Instructions

For this example the job is being run against sandbox

### Running Scripts Individually

1. Set the OKTA_CLIENT_ID and OKTA_CLIENT_PASSWORD
   ```bash
   OKTA_CLIENT_ID=<client id>
   OKTA_CLIENT_PASSWORD=<client password>
   ```
1. Create the AUTH token `AUTH=$(echo -n "${OKTA_CLIENT_ID}:${OKTA_CLIENT_PASSWORD}" | base64)`
1. Run `source bootstrap -sandbox --auth $AUTH` to set environment variables for a job.
1. Run `./start-job` to start a job. If successful a file containing
the job id will be saved in `<directory>/jobId.txt`
1. Run `./monitor-job` which will monitor the state of the running job. When the job
finished the full HTTP response will be saved to `<directory>/response.json`
1. Run `./download-results`

### Running Aggregate Script
1. Set the OKTA_CLIENT_ID and OKTA_CLIENT_PASSWORD
   ```bash
   OKTA_CLIENT_ID=<client id>
   OKTA_CLIENT_PASSWORD=<client password>
   ```
2. Create the AUTH token `AUTH=$(echo -n "${OKTA_CLIENT_ID}:${OKTA_CLIENT_PASSWORD}" | base64)`
3. Run `./run-job -sandbox --auth $AUTH` to start, monitor, and download results from a job.
