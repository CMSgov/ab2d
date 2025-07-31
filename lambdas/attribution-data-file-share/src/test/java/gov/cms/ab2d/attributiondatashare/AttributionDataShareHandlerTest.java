package gov.cms.ab2d.attributiondatashare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.lambdalibs.lib.ParameterStoreUtil;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import gov.cms.ab2d.testutils.TestContext;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;

import static gov.cms.ab2d.attributiondatashare.AttributionDataShareConstants.TEST_ENDPOINT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
class AttributionDataShareHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    private final LambdaLogger logger = mock(LambdaLogger.class);
    private final ParameterStoreUtil parameterStore = new ParameterStoreUtil("", "", "");
    private final AttributionDataShareHelper helper = mock(AttributionDataShareHelper.class);
    private final AttributionDataShareHandler handler = spy(new AttributionDataShareHandler());

    @Test
    void attributionDataShareInvoke() {
        var mockParameterStore = mockStatic(ParameterStoreUtil.class);
        mockParameterStore
                .when(() -> ParameterStoreUtil.getParameterStore(anyString(), anyString(), anyString()))
                .thenReturn(parameterStore);

        Connection dbConnection = mock(Connection.class);
        mockStatic(DriverManager.class)
                .when(() -> DriverManager.getConnection(anyString(), anyString(), anyString())).thenReturn(dbConnection);

        when(handler.helperInit(anyString(), anyString(), any(LambdaLogger.class))).thenReturn(helper);
        assertDoesNotThrow(() -> handler.handleRequest(null, System.out, new TestContext()));
    }

    @Test
    void attributionDataShareExceptionTest() {
        Exception ex = mock(Exception.class);
        when(ex.getMessage()).thenReturn("Exception");
        assertThrows(AttributionDataShareException.class, () -> handler.throwAttributionDataShareException(logger, ex));
    }

    @Test
    void getS3ClientTest() {
        assertNotNull(handler.getAsyncS3Client(TEST_ENDPOINT, parameterStore));
    }
}
