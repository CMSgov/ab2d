# Ab2d Events Client

This is a thin wrapper around the AWS SNS client to make using Localstack easier. 
It also 
 * Provides a central location so share DTO classes
 * Topics enum contains all known topics  
 * Handles appending enviroment to topic name

### Gradle

```
implementation 'gov.cms.ab2d:ab2d-sns-client:1.0'
```

### Maven

```
<dependency>
    <groupId>gov.cms.ab2d</groupId>
    <artifactId>ab2d-sns-client</artifactId>
    <version>${version}</version>
</dependency>
```

### Add New Message type or topic for ab2d-sns-client

 * Add a message object in gov/cms/ab2d/snsclient/messages
 * Add a new topics in gov/cms/ab2d/snsclient/messages/Topics





