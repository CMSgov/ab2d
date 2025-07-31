## Description 
A simple lambda that converts Cloudwatch events to messages the event service can accept. 

## Overreaching objective

AB2D occasionally has issues with services and upstream partners. 
To better track KPI these issues need to be recorded. 
There are currently 5 main areas of focus: BFD, HPMS, RDS, EFS, and API
RDS, EFS, and API are monitored by cloudwatch alerts. Whenever there's an issue the alarm triggers and sends a message to this lambda
HPMS and BFD issues are captured by the AB2D applications.

All issues are sent to the event service which stores them in a metrics table. 

There are multiple quicksight dashboards to visualize the data. 

## Build/Deploy/etc

Please see the [README.md](../README.md) in the root directory of this project.