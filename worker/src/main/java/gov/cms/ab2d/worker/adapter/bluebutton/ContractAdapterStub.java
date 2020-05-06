package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.filter.FilterOutByDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a stub implementation that we can use till the BFD API becomes available.
 * The rightmost 3 characters of the contractNumber being passed in must be numeric.
 */
@Slf4j
//@Primary    //Till the BFD api starts returning data, use this as the primary instance.
@Component
public class ContractAdapterStub implements ContractAdapter {

    private static final String BENE_ID_FILE = "/test-stub-data/synthetic-bene-ids.csv";
    private static final int MAX_ROWS = 30_000;

    @Override
    public GetPatientsByContractResponse getPatients(String contractNumber, int currentMonth) {

        final int contractSno = extractContractSno(contractNumber);

        final var patientsPerContract = fetchPatientRecords(contractSno);

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
            int contractNo = Integer.valueOf(tmpContractNo);
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
            return new ArrayList<String>();
        }

        int numberOfRows = determineNumberOfRows(contractSno);
        int rowsToRetrieve = determineRowsToRetrieve(numberOfRows);

        var patientIdRows = getFromSampleFile(rowsToRetrieve);

        int remainingRows = numberOfRows;
        List<String> accumulator = new ArrayList<>();
        while (remainingRows > MAX_ROWS) {
            accumulator.addAll(patientIdRows);
            remainingRows -= MAX_ROWS;
        }

        accumulator.addAll(patientIdRows.stream().limit(remainingRows).collect(Collectors.toList()));

        return accumulator;
    }

    private int determineNumberOfRows(int contractSno) {
        return contractSno == 0 ? 100 : contractSno * 1000;
    }

    private int determineRowsToRetrieve(int numberOfRows) {
        return numberOfRows < MAX_ROWS ? numberOfRows : MAX_ROWS;
    }

    private List<String> getFromSampleFile(int rowsToRetrieve) {
        var patientIdRows = new ArrayList<String>();

        try (var inputStream = this.getClass().getResourceAsStream(BENE_ID_FILE)) {
            Assert.notNull(inputStream, "error getting resource as stream :  " + BENE_ID_FILE);

            try (var br =  new BufferedReader(new InputStreamReader(inputStream))) {
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


    private GetPatientsByContractResponse toResponse(String contractNumber, List<String> rows) {
        return GetPatientsByContractResponse.builder()
                .contractNumber(contractNumber)
                .patients(toPatients(rows))
                .build();
    }

    private List<GetPatientsByContractResponse.PatientDTO> toPatients(List<String> rows) {
        return rows.stream()
                .map(row -> toPatientDTO(row))
                .collect(Collectors.toList());
    }


    private GetPatientsByContractResponse.PatientDTO toPatientDTO(String row) {
        return GetPatientsByContractResponse.PatientDTO.builder()
                .patientId(row)
                .dateRangesUnderContract(toMonthsUnderContract())
                .build();
    }

    /**
     * returns all 12 months in the list.
     * @return
     */
    private List<FilterOutByDate.DateRange> toMonthsUnderContract() {
        try {
            return Arrays.asList(new FilterOutByDate.DateRange(new Date(0), new Date()));
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
}
