package gov.cms.ab2d.optout;


import gov.cms.ab2d.common.model.Consent;
import gov.cms.ab2d.common.repository.ConsentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Optional;

@Slf4j
@Component
public class OptOutProcessorImpl implements OptOutProcessor {

    @Autowired
    private S3Gateway s3Gateway;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private ConsentConverterService  consentConverterService;


    @Override
    @Transactional
    public void process() {

        final InputStreamReader inputStreamReader = s3Gateway.getS3Object();

        try (var bufferedReader = new BufferedReader(inputStreamReader)) {
            importConsentRecords(bufferedReader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private void importConsentRecords(BufferedReader bufferedReader) {
        var iterator = IOUtils.lineIterator(bufferedReader);

        int lineNum = 0;
        while (iterator.hasNext()) {
            lineNum++;

            try {
                Optional<Consent> optConsent = consentConverterService.convert(iterator.nextLine(), "s3Filename", lineNum);
                if (optConsent.isPresent()) {
                    consentRepository.save(optConsent.get());
                }
            } catch (Exception e) {
                log.error("Invalid opt out record {}", lineNum, e);
            }

            log.info("[{}] rows parsed from file", lineNum);
            log.info("[{}] rows inserted into consent table", lineNum - 2);
        }
    }


}
