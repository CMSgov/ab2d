package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.processor.eob.ProgressTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createIdentifierWithoutMbi;

/**
 * This is a stub implementation that we can use till the BFD API becomes available.
 * The rightmost 3 characters of the contractNumber being passed in must be numeric.
 */
@Slf4j
//@Primary    //Till the BFD api starts returning data, use this as the primary instance.
@Component
public class ContractAdapterStub implements ContractBeneSearch {

    private static final String BENE_ID_FILE = "/test-stub-data/synthetic-bene-ids.csv";
    private static final int MAX_ROWS = 30_000;

    @Override
    public ContractBeneficiaries getPatients(String contractNumber, int currentMonth, ProgressTracker tracker) {

        final int contractSno = extractContractSno(contractNumber);

        final List<String> patientsPerContract = fetchPatientRecords(contractSno);
        for (int i = 0; i < currentMonth; i++) {
            tracker.incrementTotalContractBeneficiariesSearchFinished();
        }

        return toResponse(contractNumber, patientsPerContract);
    }

    Integer extractContractSno(String contractNumber) {
        //valid range from 0000 - 9999
        final String contractNumberSuffix = contractNumber.substring(contractNumber.length() - 4);

        Integer sno = null;
        try {
            sno = Integer.valueOf(contractNumberSuffix);
        } catch (NumberFormatException e) {
            log.warn("Invalid contractNumber : {} ", contractNumber);
            return 0;
        }

        // simple check for contractNumber with 5 digits instead of 4
        try {
            final String tmpContractNo = contractNumber.substring(contractNumber.length() - 5);
            int contractNo = Integer.parseInt(tmpContractNo);
            if (sno != contractNo) {
                sno = -1;
            }
        } catch (NumberFormatException e) {
            //ignore - this check is to see if the contractNumber has 5 digits instead of 4 making it > 9999
            log.trace("Contract number has value of {} before extraction", contractNumber);
        }

        return sno;
    }

    private List<String> fetchPatientRecords(final int contractSno) {
        if (contractSno < 0) {
            return new ArrayList<>();
        }

        int numberOfRows = determineNumberOfRows(contractSno);
        int rowsToRetrieve = determineRowsToRetrieve(numberOfRows);

        List<String> patientIdRows = getFromSampleFile(rowsToRetrieve);

        int remainingRows = numberOfRows;
        List<String> accumulator = new ArrayList<>();
        int index = 0;
        while (remainingRows > MAX_ROWS) {
            // Make sure the Patient IDs aren't used more than once. Just append an index on the values if we've
            // been through this list more than once
            if (index == 0) {
                accumulator.addAll(patientIdRows);
            } else {
                final String iString = String.valueOf(index);
                accumulator.addAll(patientIdRows.stream().map(c -> c + iString).collect(Collectors.toList()));
            }
            remainingRows -= MAX_ROWS;
            index++;
        }
        final String iString = String.valueOf(index);
        accumulator.addAll(patientIdRows.stream().limit(remainingRows).map(c -> c + iString).collect(Collectors.toList()));

        return accumulator;
    }

    private int determineNumberOfRows(int contractSno) {
        return contractSno == 0 ? 100 : contractSno * 1000;
    }

    private int determineRowsToRetrieve(int numberOfRows) {
        return Math.min(numberOfRows, MAX_ROWS);
    }

    private List<String> getFromSampleFile(int rowsToRetrieve) {
        List<String> patientIdRows = new ArrayList<>();

        try (InputStream inputStream = this.getClass().getResourceAsStream(BENE_ID_FILE)) {
            Assert.notNull(inputStream, "error getting resource as stream :  " + BENE_ID_FILE);

            try (BufferedReader br =  new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                Assert.notNull(br, "Could not create buffered reader from input stream :  " + BENE_ID_FILE);

                final List<String> rows = br.lines()
                        .limit(rowsToRetrieve)
                        // This is kind of a hack, I know, but this is a mock,
                        // and it's not going to be around for much longer anyway.
                        .map(row -> "-"
                                .concat(row))
                        .collect(Collectors.toList());

                patientIdRows.addAll(rows);
            }
        } catch (Exception ex) {
            final String errMsg = "Error reading file : ";
            log.error("{} {} ", errMsg, BENE_ID_FILE, ex);
            throw new RuntimeException(errMsg + BENE_ID_FILE);
        }

        return patientIdRows;
    }

    private ContractBeneficiaries toResponse(String contractNumber, List<String> rows) {
        ContractBeneficiaries beneficiaries = new ContractBeneficiaries();
        beneficiaries.setContractNumber(contractNumber);
        Map<String, ContractBeneficiaries.PatientDTO> patientsMap = new HashMap<>();
        List<FilterOutByDate.DateRange> monthsUnderContract;
        try {
            // returns all 12 months in the list.
            monthsUnderContract = Collections.singletonList(FilterOutByDate.getDateRange(1, 2020, 12, 2020));
        } catch (Exception ex) {
            monthsUnderContract = new ArrayList<>();
        }
        beneficiaries.setPatients(patientsMap);
        for (String row : rows) {
            ContractBeneficiaries.PatientDTO patientDTO = ContractBeneficiaries.PatientDTO.builder()
                    .identifiers(createIdentifierWithoutMbi(row))
                    .dateRangesUnderContract(monthsUnderContract)
                    .build();
            patientsMap.put(row, patientDTO);
        }
        return beneficiaries;
    }
}
