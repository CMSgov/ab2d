package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.model.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/common-it.properties")
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    public void testUser() {
        User user = userService.getCurrentUser();

        assertEquals(null, user); // no authentication for now, so will be null
    }
}
