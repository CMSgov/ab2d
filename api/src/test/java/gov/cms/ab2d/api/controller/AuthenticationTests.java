package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Map;

import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class AuthenticationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    private Map<String, String> headerMap;

    @Before
    public void setup() throws IOException, InterruptedException {
        headerMap = testUtil.setupToken();
    }

    // Negative tests, successful auth tests are essentially done in other suites
    @Test
    public void testNoAuthHeader() throws Exception {
        //{"token_type":"Bearer","expires_in":3600,"access_token":"eyJraWQiOiIwUlBkVnJBUWpKdjhwNkFRVXlGb3p2Z3lSSnlhSnhmemFURERlM2VhT3RFIiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULmN6RlljRnd6b21VYTJZLXpzckZKV2J4RkVjMkl0ajBHUC13ZDRqWUVpcUkiLCJpc3MiOiJodHRwczovL2Rldi00MTgyMTIub2t0YS5jb20vb2F1dGgyL2RlZmF1bHQiLCJhdWQiOiJhcGk6Ly9kZWZhdWx0IiwiaWF0IjoxNTc0MTA1ODA4LCJleHAiOjE1NzQxMDk0MDgsImNpZCI6IjBvYTFweGFwemRPV2lIZjl1MzU3IiwidWlkIjoiMDB1MXYxOGt3NzBlVDNjcGczNTciLCJzY3AiOlsib3BlbmlkIl0sInN1YiI6IkVpbGVlbkNGcmllcnNvbkBleGFtcGxlLmNvbSJ9.pgE8KiPUuFPdZdbRzyTgsi0VoAaKIVxFlBqrKofICoofDjuvh7z5A7jNi-U5rGcCmuUhAOhbLDvPxoPim5iTNjDZkZPlp-XtFSJg1fQp1sASUEdkMRltNzeUw60XpcWe8O79EjdakoI-lr3AUYUh5HYqAB5sjMbV0BA70yY7TB-DCcZYbjMUlTYY-QftyEfz8McvvgOftvv6PBwETLG_olDf2ymwUB7Ba5-cz_MetgUmipAEvkAReMKwgM-27w3iTInPzOJiEtjRi_0ttrMqNGWDyezUivsQX_BhtOSAlczPhmKUxIbftqcvP3g9Y3RaP6HNKBX_1lxm-fLozvkdLA","scope":"openid","id_token":"eyJraWQiOiIwUlBkVnJBUWpKdjhwNkFRVXlGb3p2Z3lSSnlhSnhmemFURERlM2VhT3RFIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIwMHUxdjE4a3c3MGVUM2NwZzM1NyIsInZlciI6MSwiaXNzIjoiaHR0cHM6Ly9kZXYtNDE4MjEyLm9rdGEuY29tL29hdXRoMi9kZWZhdWx0IiwiYXVkIjoiMG9hMXB4YXB6ZE9XaUhmOXUzNTciLCJpYXQiOjE1NzQxMDU4MDgsImV4cCI6MTU3NDEwOTQwOCwianRpIjoiSUQuZjZlSkk3dG5DVVMteDRGOHBjMHhjNENkQUw2NUtJWjQ0M2lFQ1U0YlpOWSIsImFtciI6WyJwd2QiXSwiaWRwIjoiMDBvMXBsN2JhaHBzRWJ4RmIzNTciLCJhdXRoX3RpbWUiOjE1NzQxMDU4MDgsImF0X2hhc2giOiJXbjR2bkExZFI5LURyWVlxSEg5NGl3In0.hPpAwX76lg0hZevduHxPor7uXpd9MLdeuTgCW6fc5B9sHe8vvMF7Pt-X-tSKcBLcb1K7imP3P4B1cHW-YQjk-necauoqs6_dwWRUMIHw3dRs3P0S0tidLaX8KizAR2oab2ow2VNbFeGHjWBwJIeFqdKAzKEz9tpBKW1Jc62ltUQ8LC9uAxtBm5yq0yiwq9zJWf_mFCaadGDVspooXUINVF_fnOpx0HKlgBXq5WfJhYRS0pOHg3-rfsUdgwrbjDydKUp8quDPCY5iJghmNLxqMbq2HgMfDeaXbxwHu_-eEIJ5QjsrLzXHmoUqmzZUqkavZjAVvV3jE1v6coc4nc8Zfw"}

        this.mockMvc.perform(get(API_PREFIX + "/Patient/$export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
    }
}
