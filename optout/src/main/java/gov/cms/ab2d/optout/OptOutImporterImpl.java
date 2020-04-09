package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptOutImporterImpl implements OptOutImporter {

    private final OptOutRepository optOutRepository;
    private final OptOutConverterService optOutConverterService;
    private final EventLogger eventLogger;

    @Override
    @Transactional
    public void process(BufferedReader bufferedReader, String filename) {
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

                List<OptOut> newOptOuts = optOutConverterService.convert(line);
                if (newOptOuts.size() > 1) {
                    log.info("Multiple({}) Patients for HICN {}", newOptOuts.size(), newOptOuts.get(0).getHicn());
                }

                newOptOuts.forEach(newOptOut -> saveOptOutRecord(newOptOut, filename));
                insertedRowCount = insertedRowCount + newOptOuts.size();
            } catch (Exception e) {
                log.error("Invalid opt out record - line number :[{}]", linesReadCount, e);
            }
        }
        eventLogger.log(new ReloadEvent(null, ReloadEvent.FileType.OPT_OUT, filename, insertedRowCount));

        log.info("[{}] rows read from file", linesReadCount);
        log.info("[{}] rows inserted into opt_out table", insertedRowCount);
    }

    /**
     *
     * Search the table for an opt_out record given CCW_ID and HICN
     * if none found, insert the new opt_out record.
     * if a record already exists AND the effective date is different, update the effective data and the new filename
     * Otherwise, ignore it
     *
     * @param newOptOut
     * @param filename
     */
    private void saveOptOutRecord(OptOut newOptOut, String filename) {
        newOptOut.setFilename(filename);

        final Optional<OptOut> optDbData = optOutRepository.findByCcwIdAndHicn(newOptOut.getCcwId(), newOptOut.getHicn());
        if (optDbData.isEmpty()) {
            optOutRepository.save(newOptOut);
            return;
        }

        // a row was found in the opt_out table with the same ccw_id & hicn.
        // if the effectiveDate is different, update the Effective Date & new filename
        final OptOut dbOptOut = optDbData.get();
        if (!dbOptOut.getEffectiveDate().isEqual(newOptOut.getEffectiveDate())) {
            dbOptOut.setEffectiveDate(newOptOut.getEffectiveDate());
            dbOptOut.setFilename(newOptOut.getFilename());
            optOutRepository.save(dbOptOut);
        }
    }
}
