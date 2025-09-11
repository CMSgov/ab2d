#Aggregator
### Responsibilities of the worker:

1. Creates all necessary directories (job directory, streaming directory and finished directory)
1. Starts the Aggregator Callable with the parameters:
   1. Location of EFS mount
   1. Job ID
   1. Streaming directory name - The name of the directory under the job directory where files are streamed to until they are complete
   1. Finished directory name - The name of the directory under the job directory where files that are finished streaming are placed
   1. Contract Number - To coordinate the file names for the resulting combined files
   1. Max megabytes - The maximum size of the aggregated files (currently the existing 200MB)
   1. Multiplier - Because we are aggregating the entire time the job is running, informs the aggregator when it is appropriate to start aggregating files. For example, if it is 2 and the maximum size is 200MB, start aggregating when the available files is over 400MB.
1. Cancels Aggregator thread if a job is cancelled
1. Creates threads where each thread:
   1. Search BFD for a batch of N beneficiaries
   2. Streams EOB data to a temp file in the streaming directory
   3. Write out any errors to an error file
   4. Once the file has completely finished streaming the data for the entire batch of beneficiaries, close the file and move it to the finished directory
   5. Notify the aggregator that it is done writing out data so the aggregator will know that it is okay to aggregate all remaining files. This is accomplished by removing the streaming directory
   6. Wait for the aggregator thread to complete aggregating or the thread to be "done"
   7. Create output objects for the files that exist in the job's top level directory

### Responsibilities of the aggregator:
1. Wait for there to be enough data in the finished directory (based on the multiplier and max size of aggregated file)
2. Grab an appropriate number of files in the finished directory to get as close to the max mega bytes size, combine them into the next contractnum_000x.ndjson file in the job directory and remove the tmp files from the finished directory. This occurs for both data and error files.
3. Once the worker removes the streaming directory (indicating it is done writing data for the job), aggregate any remaining temp files
4. Remove the finished directory, indicating that the job has been aggregated

### Responsibilities of the aggregator callable
1. Instantiate the Aggregator Callable
2. Attempt to aggregate data for both the data files in and error files one second intervals until the worker is done
3. Once the worker is done, aggregate the remaining, then return the number of aggregations (completing the thread)

### Example scenario:
Contract Z9000 has 85 beneficiaries and is doing job ABC-DEF:

Worker creates the directories:

```
/efsmount/Z9000
/efsmount/Z9000/streaming
/efsmount/Z9000/finished
```
Worker creates an Aggregator Callable with the following parameters:

1. Location of EFS mount - ```/efsmount```
2. Job ID - ```ABC-DEF```
3. Streaming directory name - ```streaming```
4. Finished directory name - ```finished```
5. Contract number - ```Z9000```
6. Max megabytes - ```200```
7. Multiplier - ```2```

Worker iterates across the 85 beneficiaries (10 at a time):

1. Creates a thread with at most those beneficiaries
2. Creates a temporary file in the ```/efsmount/Z9000/streaming``` directory
3. For each beneficiary:
   1. Searches BFD
   1. Writes to the temporary file
   1. Closes the temporary file and move it to ```fsmount/Z9000/finished``` directory
   1. Update progress tracker
4. When worker is done writing out data, removes ```fsmount/Z9000/streaming``` directory

Waits for the Aggregator Callable thread to complete

Retrieve the list of the files created in the ```fsmount/Z9000```irectory and create job output objects/table rows with them

In the meantime, the aggregator thread:

1. Checks in on the ```fsmount/Z9000/finished``` directory
2. While job is not finished - does it have 400 MB of data in that directory (done for both data and error files)?
3. If yes
   1. Pick up 200 MB of combined data from the available files
   2. Determine the name of the next file (such as ```Z9000_001.ndjson```)
   2. Combine the selected files, combine them into the new file in the ```/efsmount/Z9000 directory``` and delete the aggregated files from the /efsmount/Z9000/finished directory
4. If no - wait
5. Job is finished, aggregate remaining files in the ```/efsmount/Z9000/finished``` directory
6. Remove the ```/efsmount/Z9000/finished``` directory
Return the number of files aggregated

## Aggregator Library

This library is responsible for implementing the bulk of the aggregator functionality, including the convenience methods to help the worker perform some of its tasks. Useful objects/methods include:

Used to create all the necessary directories for the job

```JobHelper.workerSetUpJobDirectories(jobId, efsMount, streamingDir, finishedDir)```

Used to create all the necessary directories for the job

```JobHelper.workerFinishJob(efsMount + "/" + jobId + "/" + streamingDir);```

Used to manage writing out data - ClaimsStream which handles creating the file in the correct location and when closed, copies that file to the right location

```
try (ClaimsStream stream = new ClaimsStream(jobId, efsMount, false, this.streamingDir, this.finishedDir)) {
    for (CoverageSummary patient : patients) {
        List<IBaseResource> eobs = getEobBundleResources(request, patient);
        for (IBaseResource resource : eobs) {
            stream.write(parser.encodeResourceToString(resource) + System.lineSeparator());
        }
    }
}
```

AggregatorCallable is used to fully implement aggregation in a thread:

```
AggregatorCallable aggregator = new AggregatorCallable(efsMount, jobId, contractNumber,
    ndjsonRollOver, streamingDir, finishedDir, multiplier);

JobHelper.workerSetUpJobDirectories(job.getJobUuid(), efsMount, streamingDir, finishedDir);
    
Future<Integer> aggregatorFuture = aggregatorThreadPool.submit(aggregator);

// Do all the work of creating the search/write threads
lotsOfSearchingSerializingStuff();
 
// Mark the job as finished for the aggregator (all file data has been written out)
JobHelper.workerFinishJob(efsMount + "/" + job.getJobUuid() + "/" + streamingDir);

// Wait for the aggregator to finish
while (!aggregatorFuture.isDone()) {
    Thread.sleep(1000);
}
```
