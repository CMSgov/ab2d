package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOutFile;
import gov.cms.ab2d.common.repository.OptOutFileRepository;
import gov.cms.ab2d.common.service.OptOutFileService;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.optout.gateway.S3Gateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptOutProcessorImpl implements OptOutProcessor {

    private final S3Gateway s3Gateway;
    private final OptOutImporter optOutImporter;
    private final OptOutFileService optOutFileService;
    private final OptOutFileRepository optOutFileRepository;

    @Value("${optout.used}")
    private boolean optOutUsed;

    @Override
    public void process() {
        if (optOutUsed) {
            final List<String> filenames = s3Gateway.listOptOutFiles();
            filenames.stream().forEach(this::fetchAndProcessOptOutFile);
        }
    }

    private void fetchAndProcessOptOutFile(final String filename) {
        if (optOutFileProcessed(filename)) {
            log.info("Skipping import of file [{}]", filename);
            return;
        }

        try (var inputStreamReader = s3Gateway.getOptOutFile(filename);
             var bufferedReader = new BufferedReader(inputStreamReader)) {

            log.info("importing [{}]", filename);

            optOutImporter.process(bufferedReader, filename);

            OptOutFile optOutFile = new OptOutFile();
            optOutFile.setFilename(filename);
            optOutFileRepository.save(optOutFile);

            log.info("[{}] - import completed successfully", filename);
        } catch (Exception e) {
            log.error("[{}] - import FAILED ", filename, e);
        }
    }

    private boolean optOutFileProcessed(String filename) {
        try {
            optOutFileService.findByFilename(filename);
        } catch (ResourceNotFoundException exception) {
            return false;
        }

        return true;
    }
}
