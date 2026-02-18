package gov.cms.ab2d.api.controller.v1;

import gov.cms.ab2d.api.controller.ErrorHandler;
import gov.cms.ab2d.api.controller.common.FileDownloadCommon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

@WebMvcTest(FileDownloadAPIV1.class)
@ContextConfiguration(classes = {FileDownloadAPIV1.class})
public class FileDownloadAPIV1Test {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	FileDownloadCommon fileDownloadCommon;

	@MockitoBean
	ErrorHandler errorHandler;

	@Test
	@WithMockUser
	void blah() throws Exception {
		when(fileDownloadCommon.downloadFile(eq("1234"), eq("test.json"), any(), any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));

		mockMvc.perform(get("/api/v1/fhir/Job/1234/file/test.json"))
				.andExpect(status().isOk());

	}

}
