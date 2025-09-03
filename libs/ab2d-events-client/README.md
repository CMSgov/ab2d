# Ab2d Events Client

This library allow other services to send Events into the Event SQS Queue.

### Apply Client to Service

### Gradle
```
implementation 'gov.cms.ab2d:ab2d-events-client:1.0'
```

### Maven
```
<dependency>
    <groupId>gov.cms.ab2d</groupId>
    <artifactId>ab2d-events-client</artifactId>
    <version>1.0</version>
</dependency>
```

### Add New Message type for ab2d-events-client
1. Add a message object in gov/cms/ab2d/eventclient/messages
```
@Data
public class MyMessageExample extends SQSMessages

# All events/logs variables you want to send to the event service.
private LoggableEvent exampleObject;
```

2. Add a new method to send messages to SQS in SQSEventClient.java (see java file for examples)
3. Upgrade the event client version in /AB2D-Filters/build.gradle to build changes
```
# Increase current version
eventClientVersion='1.0'
```
4. Modify the [Event Service Repo](https://github.com/CMSgov/ab2d-events) to absorb message.




