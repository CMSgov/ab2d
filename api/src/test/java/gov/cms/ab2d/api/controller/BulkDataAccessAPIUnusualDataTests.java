package gov.cms.ab2d.api.controller;

import com.amazonaws.services.mediaconnect.model.Messages;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.remote.JobClientMock;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.eventclient.clients.SQSConfig;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ContractSearchEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.eventclient.events.FileEvent;
import gov.cms.ab2d.eventclient.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventclient.events.ReloadEvent;
import gov.cms.ab2d.eventclient.messages.GeneralSQSMessage;
import gov.cms.ab2d.eventclient.messages.SQSMessages;
import gov.cms.ab2d.eventclient.messages.TraceAndAlertSQSMessage;
import gov.cms.ab2d.job.dto.StartJobDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import gov.cms.ab2d.common.util.UtilMethods;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
    private ContractRepository contractRepository;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    SQSEventClient sqsEventClient;

    @Autowired
    AmazonSQS amazonSQS;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
        jobClientMock.cleanupAll();
        amazonSQS.purgeQueue(new PurgeQueueRequest(System.getProperty("sqs.queue-name")));
    }

    @Test
    void testPatientExportWithNoAttestation() throws Exception {
        // Valid contract number for sponsor, but no attestation
        String token = testUtil.setupContractWithNoAttestation(List.of(SPONSOR_ROLE));
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));

        ApiRequestEvent requestEvent = (ApiRequestEvent) SQSConfig.objectMapper().readValue(amazonSQS.receiveMessage(System.getProperty("sqs.queue-name")).getMessages().get(0).getBody(), GeneralSQSMessage.class).getLoggableEvent();
        ErrorEvent errorEvent = (ErrorEvent) SQSConfig.objectMapper().readValue(amazonSQS.receiveMessage(System.getProperty("sqs.queue-name")).getMessages().get(0).getBody(), GeneralSQSMessage.class).getLoggableEvent();
        ApiResponseEvent responseEvent = (ApiResponseEvent) SQSConfig.objectMapper().readValue(amazonSQS.receiveMessage(System.getProperty("sqs.queue-name")).getMessages().get(0).getBody(), TraceAndAlertSQSMessage.class).getLoggableEvent();

        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());
        assertEquals(ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT, errorEvent.getErrorType());

        // expect no more messages
        assertEquals(0, amazonSQS.receiveMessage(System.getProperty("sqs.queue-name")).getMessages().size(), 0);
    }

    @Test
    public void testPatientExportWithOnlyParentAttestation() throws Exception {
        String token = testUtil.setupContractSponsorForParentClientData(List.of(SPONSOR_ROLE));

        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
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
