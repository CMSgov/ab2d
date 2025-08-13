## Description 
Retrieve coverage counts from the contract service and send them to the coverage count lambda. 

## Goal
The contract service pulls contract information from HPMS on a schedule. That information also contains the coverage counts.
Instead of adding another schedule and more dependencies to the contract service have a lambda handle requesting count updates.    

## Overall design

Make a GET request to contract service's /contracts endpoint. Loop through the results and extract the contract number and MedicareEligible count.
Since HPMS only provides the current month's counts find the systems current month and year.
Load all contract/count/year/month into a list of new CoverageCountDTO and send them to the coverage-counts lambda for storage. 

