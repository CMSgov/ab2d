package gov.cms.ab2d.optout;


import gov.cms.ab2d.common.model.Consent;
import gov.cms.ab2d.common.repository.ConsentRepository;
import gov.cms.ab2d.optout.gateway.S3Gateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

        final InputStreamReader inputStreamReader = s3Gateway.getOptOutFile();

        try (var bufferedReader = new BufferedReader(inputStreamReader)) {
            importConsentRecords(bufferedReader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }


    private void importConsentRecords(BufferedReader bufferedReader) {
        var iterator = IOUtils.lineIterator(bufferedReader);

        int linesReadCount = 0;
        int insertedRowCount = 0;
        while (iterator.hasNext()) {
            ++linesReadCount;

            try {
                final String line = iterator.nextLine();
                if (StringUtils.isBlank(line)) {
                    log.warn("Blank line in file. Skipping.");
                    continue;
                }

                Optional<Consent> optConsent = consentConverterService.convert(line);
                if (optConsent.isPresent()) {
                    consentRepository.save(optConsent.get());
                    ++insertedRowCount;
                }
            } catch (Exception e) {
                log.error("Invalid opt out record - line number :[{}]", linesReadCount, e);
            }

        }

        log.info("[{}] rows read from file", linesReadCount);
        log.info("[{}] rows inserted into consent table", insertedRowCount);
    }





}
