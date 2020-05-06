package gov.cms.ab2d.optout;

import gov.cms.ab2d.optout.gateway.S3Gateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptOutProcessorImpl implements OptOutProcessor {

    private final S3Gateway s3Gateway;
    private final OptOutImporter optOutImporter;

    @Override
    public void process() {
        final List<String> filenames = s3Gateway.listOptOutFiles();
        filenames.stream().forEach(this::fetchAndProcessOptOutFile);
    }

    private void fetchAndProcessOptOutFile(final String filename) {
        try (var inputStreamReader = s3Gateway.getOptOutFile(filename);
             var bufferedReader = new BufferedReader(inputStreamReader)) {

            log.info("importing [{}]", filename);

            optOutImporter.process(bufferedReader, filename);

            log.info("[{}] - import completed successfully", filename);
        } catch (Exception e) {
            log.error("[{}] - import FAILED ", filename, e);
        }
    }
}
