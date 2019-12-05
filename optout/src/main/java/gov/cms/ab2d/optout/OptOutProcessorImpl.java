package gov.cms.ab2d.optout;


import gov.cms.ab2d.common.model.Consent;
import gov.cms.ab2d.common.repository.ConsentRepository;
import gov.cms.ab2d.optout.gateway.S3Gateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class OptOutProcessorImpl implements OptOutProcessor {

    private final S3Gateway s3Gateway;
    private final ConsentRepository consentRepository;
    private final ConsentConverterService  consentConverterService;



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

        int linesRead = 0;
        int insertedRowCount = 0;
        int skippedRowCount = 0;
        while (iterator.hasNext()) {
            ++linesRead;

            try {
                final String line = iterator.nextLine();

                Optional<Consent> optConsent = consentConverterService.convert(line, linesRead);
                if (optConsent.isPresent()) {
                    consentRepository.save(optConsent.get());
                    ++insertedRowCount;
//                } else {
//                    ++skippedRowCount;
//                    log.info("line not used : {} : row : {} ", line, linesRead);
                }
            } catch (Exception e) {
                log.error("Invalid opt out record {}", linesRead, e);
            }

        }

        log.info("[{}] rows parsed from file", linesRead);
        log.info("[{}] lines skipped", skippedRowCount);
        log.info("[{}] rows inserted into consent table", insertedRowCount);
    }





}
