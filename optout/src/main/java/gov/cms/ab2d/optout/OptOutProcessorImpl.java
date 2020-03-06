package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.optout.gateway.S3Gateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptOutProcessorImpl implements OptOutProcessor {

    private final S3Gateway s3Gateway;
    private final OptOutRepository optOutRepository;
    private final OptOutConverterService optOutConverterService;

    @Override
    @Transactional
    public void process() {

        final List<String> filenames = s3Gateway.listOptOutFiles();
        logFileNames(filenames);

        filenames.stream().forEach(fileName -> fetchOptOutFile(fileName));

    }

    private void logFileNames(List<String> filenames) {

        log.info("The following files were found in S3 ...");
        filenames.stream()
                .forEach(file -> log.info("{}", file));
    }

    private void fetchOptOutFile(final String filename) {
        try (var inputStreamReader = s3Gateway.getOptOutFile(filename);
             var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            log.info("importing file : {}", filename);
            importOptOutRecords(bufferedReader);

            log.info("imported file : {} - DONE", filename);
        } catch (IOException e) {
            log.error("import opt-out data - FAILED.");
            throw new UncheckedIOException(e);
        }
    }

    private void importOptOutRecords(BufferedReader bufferedReader) {
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

                List<OptOut> optOuts = optOutConverterService.convert(line);
                if (optOuts.size() > 1) {
                    log.info("Multiple({}) Patients for HICN {}", optOuts.size(), optOuts.get(0).getHicn());
                }

                optOuts.forEach(o -> optOutRepository.save(o));
                insertedRowCount = insertedRowCount + optOuts.size();
            } catch (Exception e) {
                log.error("Invalid opt out record - line number :[{}]", linesReadCount, e);
            }
        }

        log.info("[{}] rows read from file", linesReadCount);
        log.info("[{}] rows inserted into opt_out table", insertedRowCount);
    }
}
