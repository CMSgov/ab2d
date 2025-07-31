package gov.cms.ab2d.retrievehpmscounts;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;
import gov.cms.ab2d.testutils.AB2DLocalstackContainer;
import gov.cms.ab2d.testutils.TestContext;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@Testcontainers
class InvokeTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    private final ObjectMapper mapper = JsonMapper.builder()
            .configure(ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
            .build()
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(new JavaTimeModule())
            .registerModule(new JodaModule());

    @BeforeAll
    static void setup() {
    }


    @Test
    void
    hpmsCountInvoke() throws IOException {
        Properties props = PropertiesUtil.loadProps();

        ContractDTO contractDTO = new ContractDTO(1L, "test.json", "test.json", OffsetDateTime.now(), Contract.ContractType.NORMAL, 100, 100);
        ContractDTO contractDTO2 = new ContractDTO(2L, "test2", "test2", OffsetDateTime.now(), Contract.ContractType.NORMAL, 200, 200);
        CloseableHttpClient httpResponse = mock(CloseableHttpClient.class);
        HPMSCountsHandler eventHandler = new HPMSCountsHandler(httpResponse, getSNSClient(props.get("AWS_SNS_URL") + ""));
        Mockito.when(httpResponse.execute(any(), any(BasicHttpClientResponseHandler.class))).thenReturn(mapper.writeValueAsString(List.of(contractDTO, contractDTO2)));
        String value = mapper.writeValueAsString("");
        eventHandler.handleRequest(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)), System.out, new TestContext());
    }

    @Test
    void hpmsCountInvokeSend() throws IOException {
        Properties props = PropertiesUtil.loadProps();

        ContractDTO contractDTO = new ContractDTO(1l, "test.json", "test.json", OffsetDateTime.now(), Contract.ContractType.NORMAL, 200, 200);
        ContractDTO contractDTO2 = new ContractDTO(2l, "test2", "test2", OffsetDateTime.now(), Contract.ContractType.NORMAL, 300, 300);
        CloseableHttpClient httpResponse = mock(CloseableHttpClient.class);
        HPMSCountsHandler eventHandler = new HPMSCountsHandler(httpResponse, getSNSClient(props.get("AWS_SNS_URL") + ""));
        Mockito.when(httpResponse.execute(any(), any(BasicHttpClientResponseHandler.class))).thenReturn(mapper.writeValueAsString(List.of(contractDTO, contractDTO2)));
        String value = mapper.writeValueAsString("");
        eventHandler.handleRequest(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)), System.out, new TestContext());
    }

    private AmazonSNSClient getSNSClient(String address) {
        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
        return (AmazonSNSClient) AmazonSNSClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder
                        .EndpointConfiguration("https://" + address,
                        Regions.US_EAST_1.getName()))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("access_key_id", "secret_key_id")))
                .build();
    }


}
