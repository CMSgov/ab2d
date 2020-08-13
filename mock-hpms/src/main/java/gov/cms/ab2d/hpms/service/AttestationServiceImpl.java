package gov.cms.ab2d.hpms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Slf4j
@Service
public class AttestationServiceImpl implements AttestationService {

    private final ResourceLoader resourceLoader;

    @Autowired
    public AttestationServiceImpl(ResourceLoader resLoad) {
        this.resourceLoader = resLoad;
    }

    public List<String> retrieveAttestations(List<String> contractIds) {
        try {
            return contractIds.stream().map(this::loadContract).collect(Collectors.toList());
        } catch (IllegalArgumentException iae) {
            return emptyList();
        }
    }

    private String loadContract(String contractId) {
        String location = "classpath:attestations/" + contractId + ".json";
        try (InputStream is = resourceLoader.getResource(location).getInputStream()) {
            return StreamUtils.copyToString(is, Charset.defaultCharset());
        } catch (IOException ioe) {
            log.warn("Could not load contract id:" + contractId);
            throw new IllegalArgumentException("Could not load contract id:" + contractId);
        }
    }
}
