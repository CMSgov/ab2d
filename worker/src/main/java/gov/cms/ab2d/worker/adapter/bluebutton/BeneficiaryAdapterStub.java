package gov.cms.ab2d.worker.adapter.bluebutton;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Month;
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

    private static final String DASH_LINE = "-------------------------------------------------------------------------";
    private static final String FAKEFILEPATH = "/test-stub-data/fake-bene-ids.csv";


    @Override
    public GetPatientsByContractResponse getPatientsByContract(String contractNumber) {

        final int contractSno = extractContractSno(contractNumber);

        final int startOffset = contractSno * 100;
        final int endOffset = startOffset +  100;
        log.info("OFFSETS:  startOffset: {} -  endOffset: {} ", startOffset, endOffset);
        log.info(DASH_LINE);

        final List<String> sampleTestBenes = readBeneficiariesFromSampleFile();
        final List<String> patientsPerContract = sampleTestBenes.subList(startOffset, endOffset);

        return toResponse(contractNumber, patientsPerContract);
    }


    private Integer extractContractSno(String contractNumber) {
        final String contractNumberSuffix = contractNumber.substring(contractNumber.length() - 3);

        final Integer sno;
        try {
            sno = Integer.valueOf(contractNumberSuffix);
        } catch (NumberFormatException e) {
            final String errMsg1 = String.format("Invalid ContractNumber : %s.  ", contractNumber);
            final String errMsg2 = "The rightmost 3 characters of the contract number must be numeric. ";
            throw new IllegalArgumentException(errMsg1 + errMsg2);
        }
        if (sno < 0 || sno > 299) {
            final String errMsg = "The rightmost 3 characters of the serial number must be between 0 and 299";
            throw new IllegalArgumentException(errMsg);
        }

        log.info(DASH_LINE);
        log.info("Contract Sno : {}", sno);

        return sno;
    }

//    private List<String> readBeneficiariesFromSampleFile() {
//
//        try (InputStream inputStream = this.getClass().getResourceAsStream(FAKEFILEPATH);
//             BufferedReader br =  new BufferedReader(new InputStreamReader(inputStream));
//        ) {
//            Assert.notNull(inputStream, "Could not find and load file :  " + FAKEFILEPATH);
//            return br.lines().collect(Collectors.toList());
//        } catch (Exception ex) {
//            final String errMsg = "Error reading file : ";
//            log.error("{} {} ", errMsg, FAKEFILEPATH, ex);
//            throw new RuntimeException(errMsg + FAKEFILEPATH);
//        }
//    }

    private List<String> readBeneficiariesFromSampleFile() {

        try (var inputStream = this.getClass().getResourceAsStream(FAKEFILEPATH)) {
            Assert.notNull(inputStream, "error getting resource as stream :  " + FAKEFILEPATH);

            try (var br =  new BufferedReader(new InputStreamReader(inputStream))) {
                Assert.notNull(br, "Could not create buffered reader from input stream :  " + FAKEFILEPATH);
                return br.lines().collect(Collectors.toList());
            }
        } catch (Exception ex) {
            final String errMsg = "Error reading file : ";
            log.error("{} {} ", errMsg, FAKEFILEPATH, ex);
            throw new RuntimeException(errMsg + FAKEFILEPATH);
        }
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


//    /**
//     * Test using the main method.
//     *
//     * @param args
//     */
    public static void main(String[] args) {
        BeneficiaryAdapterStub stub = new BeneficiaryAdapterStub();
        final var response = stub.getPatientsByContract("S0100");

        log.info(DASH_LINE);
        log.info("RESPONSE : {} ", response);
        log.info(DASH_LINE);
    }

}
