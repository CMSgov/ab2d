package gov.cms.ab2d.snsclient.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES;

@Slf4j
public class SNSClientImpl implements SNSClient {
    private final SnsClient amazonSNSClient;
    private final ObjectMapper mapper = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .build();

    private final Ab2dEnvironment ab2dEnvironment;

    public SNSClientImpl(SnsClient amazonSNSClient, Ab2dEnvironment ab2dEnvironment) {
        this.amazonSNSClient = amazonSNSClient;
        this.ab2dEnvironment = ab2dEnvironment;
    }

    /***
     * Serializes and sends objects to the specified topic
     * @param topicName Name of topic without the environment appended. This method handles adding the environment
     * @param message Any object that can be serialized. SNS imposes size limitations that this method does not check for.
     *Although any object CAN be sent, it's preferable to add your object(s) in /snsclent/messages to promote code sharing
     * @throws JsonProcessingException Thrown when the provided object cannot be serialized
     */
    @Override
    public void sendMessage(String topicName, Object message) throws JsonProcessingException {
        try {
            PublishRequest request = PublishRequest.builder()
                    .message(mapper.writeValueAsString(message))
                    .topicArn(createSNSTopic(topicName))
                    .build();

            amazonSNSClient.publish(request);
        }
        catch (SnsException e) {
            log.error(e.getMessage());
        }
    }

    // map from legacy to greenfield
    static String getTopicPrefix(Ab2dEnvironment environment) {
        final String env;
        if (environment.getName().contains("dev")) {
            env="dev";
        } else if (environment.getName().contains("impl")) {
            env="test";
        } else if (environment.getName().contains("sandbox")) {
            env="sandbox";
        } else if (environment.getName().contains("prod")) {
            env="prod";
        } else {
            env=environment.getName();
        }
        return "ab2d-" + env;
    }

    private String createSNSTopic(String topicName) {
        CreateTopicResponse result;
        try {
            CreateTopicRequest request = CreateTopicRequest.builder()
                    .name(getTopicPrefix(ab2dEnvironment) + "-" + topicName)
                    .build();

            result = amazonSNSClient.createTopic(request);
            return result.topicArn();

        } catch (SnsException e) {
            log.info(e.getMessage());
        }
        return "";
    }
}
