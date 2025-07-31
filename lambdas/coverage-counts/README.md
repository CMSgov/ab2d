## Description 
Receives coverage counts from other services through SNS messages.

## Goal
Occasionally the data AB2D pulls from BFD is incorrect. HPMS also maintains current bene counts.
By comparing the counts from BFD and HPMS it's possible to detect these data issues. 
AB2D's counts before updating from BFD are also included to check if AB2S has already been affected by incorrect data. 

## Overall design

New beneficiaries are pulled from BFD once a week for the past 3 months. AB2D workers currently handle this task.
Before initiating the pull, workers will send current counts for all contracts that will be updated. 
After each contract/year/month task is completed the counts will be sent to coverage-counts lambda.

HPMS counts will flow from the contract service. A separate lambda will be scheduled to request the counts from contract service then forward them to this lambda.  


## Build/Deploy/etc

Please see the [README.md](../README.md) in the root directory of this project.