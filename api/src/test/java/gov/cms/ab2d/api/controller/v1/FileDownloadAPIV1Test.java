package gov.cms.ab2d.api.controller.v1;

import com.okta.jwt.AccessTokenVerifier;
import gov.cms.ab2d.api.controller.ErrorHandler;
import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.api.controller.common.FileDownloadCommon;
import gov.cms.ab2d.api.security.JwtConfig;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileDownloadAPIV1.class)
@ContextConfiguration(classes = {FileDownloadAPIV1.class})
public class FileDownloadAPIV1Test {

	@Autowired
	private MockMvc mockMvc;

//	@MockitoBean
//	SQSEventClient sqsEventClient;
//
//	@MockitoBean
//	ApiCommon apiCommon;
//
	@MockitoBean
	FileDownloadCommon fileDownloadCommon;

//	@MockitoBean
//	AccessTokenVerifier accessTokenVerifier;
//
//	@MockitoBean
//	PdpClientService pdpClientService;
//
//	@MockitoBean
//	JwtConfig jwtConfig;
//
	@MockitoBean
	ErrorHandler errorHandler;

	@Test
	void blah() {

	}

}
