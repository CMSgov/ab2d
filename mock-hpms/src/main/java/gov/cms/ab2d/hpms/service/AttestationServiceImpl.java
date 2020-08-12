package gov.cms.ab2d.hpms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttestationServiceImpl implements AttestationService {

    private final ResourceLoader resourceLoader;

    @Autowired
    public AttestationServiceImpl(ResourceLoader resLoad) {
        this.resourceLoader = resLoad;
    }

    public List<String> retrieveAttestations(List<String> contractIds) {
        return contractIds.stream().map(this::loadContract).collect(Collectors.toList());
    }

    private String loadContract(String contractId) {
        String location = "classpath:attestations/" + contractId + ".json";
        try (InputStream is = resourceLoader.getResource(location).getInputStream()) {
            return StreamUtils.copyToString(is, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load contract id:" + contractId);
        }
    }
}
