package gov.cms.ab2d.snsclient.clients;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface SNSClient {

    void sendMessage(String topicName, Object message) throws JsonProcessingException;

}
