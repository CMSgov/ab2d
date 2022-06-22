package gov.cms.ab2d.worker.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class LocalstackConfig {

    public static AwsClientBuilder<?, ?> configureBuilder(AwsClientBuilder<?, ?> builder) {

        String localstackUrl = System.getProperty("LOCALSTACK_URL");
        log.info("LOCALSTACK_URL: " + localstackUrl);
        if (null != localstackUrl) {
            builder
                    .withEndpointConfiguration(getEndpointConfig(localstackUrl))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("a", "")));
        } else {
            // Property name is incorrect
            builder.withCredentials(new DefaultAWSCredentialsProviderChain());
        }
        return builder;
    }

    private static AwsClientBuilder.EndpointConfiguration getEndpointConfig(String localstackURl) {
        return new AwsClientBuilder.EndpointConfiguration(localstackURl, Regions.US_EAST_1.getName());
    }
}
