package gov.cms.ab2d.worker.adapter.bluebutton;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a stub implementation that we can use till the BFD API becomes available.
 * The rightmost 3 characters of the contractNumber being passed in must be numeric.
 */
@Slf4j
@Component
public class BeneficiaryAdapterStub implements BeneficiaryAdapter {

    private static final String BENE_ID_FILE = "/test-stub-data/synthetic-bene-ids.csv";
    private static final int MAX_ROWS = 30_000;

    @Override
    public GetPatientsByContractResponse getPatientsByContract(String contractNumber) {

        final int contractSno = extractContractSno(contractNumber);

        final var patientsPerContract = getFromSampleFile(contractSno);

        return toResponse(contractNumber, patientsPerContract);
    }


    private Integer extractContractSno(String contractNumber) {
        //valid range from 0000 - 9999
        final String contractNumberSuffix = contractNumber.substring(contractNumber.length() - 4);

        Integer sno = null;
        try {
            sno = Integer.valueOf(contractNumberSuffix);
        } catch (NumberFormatException e) {
            log.warn("Invalid contractNumber : {} ", contractNumber);
            return 0;
        }

        if (sno == 0) {
            try {
                final String cnoSfx = contractNumber.substring(contractNumber.length() - 5);
                sno = Integer.valueOf(cnoSfx);
            } catch (NumberFormatException e) {
                log.warn("Invalid contractNumber : {} ", contractNumber);
                // do nothing as this was a check to make sure if the sno > 9999.
            }
        }

        return sno;
    }

    private List<String> getFromSampleFile(final int contractSno) {
        if (contractSno < 0 || contractSno > 9_999) {
            return new ArrayList<String>();
        }

        int numberOfRows = contractSno * 1000;
        int rowsToRetrieve = numberOfRows < MAX_ROWS ? numberOfRows : MAX_ROWS;

        var patientIdRows = new ArrayList<String>();
        try (var inputStream = this.getClass().getResourceAsStream(BENE_ID_FILE)) {
            Assert.notNull(inputStream, "error getting resource as stream :  " + BENE_ID_FILE);

            try (var br =  new BufferedReader(new InputStreamReader(inputStream))) {
                Assert.notNull(br, "Could not create buffered reader from input stream :  " + BENE_ID_FILE);

                patientIdRows.addAll(readLines(br, rowsToRetrieve));
            }
        } catch (Exception ex) {
            final String errMsg = "Error reading file : ";
            log.error("{} {} ", errMsg, BENE_ID_FILE, ex);
            throw new RuntimeException(errMsg + BENE_ID_FILE);
        }

        final List<String> allRows = new ArrayList<>();
        allRows.addAll(patientIdRows);

        if (numberOfRows > MAX_ROWS) {
            allRows.addAll(createMoreRows(numberOfRows, rowsToRetrieve, patientIdRows));
        }

        return allRows;
    }

    private List<String> createMoreRows(int numberOfRows, int rowsToRetrieve, ArrayList<String> patientIdRows) {
        final int diff = numberOfRows - rowsToRetrieve;
        if (diff < MAX_ROWS) {
            return patientIdRows.stream().limit(diff).collect(Collectors.toList());
        } else {
            List<String> accumulator = new ArrayList<>();
            accumulator.addAll(patientIdRows);

            final List<String> moreRows = createMoreRows(diff, rowsToRetrieve, patientIdRows);
            accumulator.addAll(moreRows);

            return accumulator;
        }
    }

    private List<String> readLines(BufferedReader br, int rowsToRetrieve) {
        return br.lines()
                .limit(rowsToRetrieve)
                .collect(Collectors.toList());
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
                .monthsUnderContract(toMonthsUnderContract())
                .build();
    }

    /**
     * returns all 12 months in the list.
     * @return
     */
    private List<Integer> toMonthsUnderContract() {
        return Arrays.asList(Month.values()).stream()
                .map(m -> m.getValue())
                .collect(Collectors.toList());
    }


}
