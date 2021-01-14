package gov.cms.ab2d.bfd.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SpringBootApp.class, properties = "bfd.keystore.location=/tmp/bb.keystore")
@ContextConfiguration(initializers = {BFDMockServerConfigurationUtil.PropertyOverrider.class})
public class BFDClientConfigurationTest {

    @Autowired
    private KeyStore keyStore;

    static {
        try {
            Files.copy(Paths.get(ClassLoader.getSystemResource("bb.keystore").toURI()),
                    Paths.get("/tmp/bb.keystore"), StandardCopyOption.REPLACE_EXISTING);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Ensure that the keystore is loaded properly when we set the property for it to be a file on the filesystem
    @Test
    public void keystoreFile() {
        assertNotNull(keyStore);
    }
}
