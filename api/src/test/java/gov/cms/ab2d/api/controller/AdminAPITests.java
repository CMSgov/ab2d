package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class AdminAPITests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testUploadHPMSFile() throws Exception {
        // Simple test to test API, more detailed test is found in service test
        MockMultipartFile multipartFile = new MockMultipartFile("file", "upload.xls",
                "application/vnd.ms-excel", "SponsorFile".getBytes());
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_PREFIX + "/uploadHPMSFile")
                .file(multipartFile))
                .andExpect(status().is(202));
    }
}
