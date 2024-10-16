package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.remote.JobClientMock;
import gov.cms.ab2d.common.service.ContractServiceStub;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.eventclient.clients.SQSConfig;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import gov.cms.ab2d.eventclient.messages.TraceAndAlertSQSMessage;
import gov.cms.ab2d.job.dto.StartJobDTO;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.Optional;


import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class BulkDataAccessAPIUnusualDataTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    JobClientMock jobClientMock;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private ContractServiceStub contractServiceStub;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    SQSEventClient sqsEventClient;

    @Autowired
    SqsAsyncClient amazonSQS;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
        jobClientMock.cleanupAll();
        amazonSQS.purgeQueue(PurgeQueueRequest.builder().queueUrl(System.getProperty("sqs.queue-name")).build());
    }

 //   @Test
    void testPatientExportWithNoAttestation() throws Exception {
        // Valid contract number for sponsor, but no attestation
        String token = testUtil.setupContractWithNoAttestation(List.of(SPONSOR_ROLE));
        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(System.getProperty("sqs.queue-name"))
                .build();
        ApiRequestEvent requestEvent = (ApiRequestEvent) SQSConfig.objectMapper().readValue(amazonSQS.receiveMessage(receiveMessageRequest).join().messages().get(0).body(), GeneralSQSMessage.class).getLoggableEvent();
        ErrorEvent errorEvent = (ErrorEvent) SQSConfig.objectMapper().readValue(amazonSQS.receiveMessage(receiveMessageRequest).join().messages().get(0).body(), GeneralSQSMessage.class).getLoggableEvent();
        ApiResponseEvent responseEvent = (ApiResponseEvent) SQSConfig.objectMapper().readValue(amazonSQS.receiveMessage(receiveMessageRequest).join().messages().get(0).body(), TraceAndAlertSQSMessage.class).getLoggableEvent();

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());
        assertEquals(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT, errorEvent.getErrorType());

        // expect no more messages
        assertEquals(0, amazonSQS.receiveMessage(receiveMessageRequest).get().messages().size(), 0);
    }

    @Test
    public void testPatientExportWithOnlyParentAttestation() throws Exception {
        String token = testUtil.setupContractSponsorForParentClientData(List.of(SPONSOR_ROLE));

        Optional<Contract> contractOptional = contractServiceStub.getContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        ResultActions resultActions = this.mockMvc.perform(
                        get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andDo(print());

        String jobUuid = jobClientMock.pickAJob();
        StartJobDTO startJobDTO = jobClientMock.lookupJob(jobUuid);
        String statusUrl =
                "http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + jobUuid + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals("http://localhost" + API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export",
                startJobDTO.getUrl());
        assertNull(startJobDTO.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT).getOrganization(), startJobDTO.getOrganization());
    }
}
