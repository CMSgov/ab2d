package gov.cms.ab2d.contracts.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractTest {
    private static final String CONTRACT_NAME = "Name";
    private static final String PARENT_NAME = "Parent";
    private static final String MARKETING_NAME = "Marketing Name";
    private static final String CONTRACT_NUMBER = "S12345";
    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:m:s");
    private static final String NOW_STRING = NOW.format(FORMATTER).toString();
    private static final Long ID = 1L;
    private static final Long PARENT_ID = 2L;
    private static final Integer MEDICARE_ELIGIBLE = 95;
    private static final Integer TOTAL_ENROLLMENT = 100;

    private Contract contract;

    @BeforeEach
    void init() {
        contract = new Contract();
        contract.setContractName(CONTRACT_NAME);
        contract.setContractType(Contract.ContractType.NORMAL);
        contract.setContractNumber(CONTRACT_NUMBER);
        contract.setAttestedOn(NOW);
        contract.setId(ID);
        contract.setHpmsParentOrg(PARENT_NAME);
        contract.setHpmsParentOrgId(PARENT_ID);
        contract.setMedicareEligible(MEDICARE_ELIGIBLE);
        contract.setTotalEnrollment(TOTAL_ENROLLMENT);
        contract.setHpmsOrgMarketingName(MARKETING_NAME);
        contract.setCreated(NOW);
        contract.setModified(NOW);
        contract.setUpdateMode(Contract.UpdateMode.AUTOMATIC);
    }

    @Test
    void testContract() {
        assertTrue(contract.hasChanges(MARKETING_NAME, PARENT_ID, PARENT_NAME, MARKETING_NAME,
                TOTAL_ENROLLMENT, MEDICARE_ELIGIBLE));
        assertTrue(contract.hasChanges(MARKETING_NAME, PARENT_ID, PARENT_NAME, MARKETING_NAME,
                TOTAL_ENROLLMENT, MEDICARE_ELIGIBLE - 1));
        assertTrue(contract.hasChanges(MARKETING_NAME, PARENT_ID, PARENT_NAME, MARKETING_NAME,
                TOTAL_ENROLLMENT - 1, MEDICARE_ELIGIBLE));
        assertTrue(contract.hasChanges(MARKETING_NAME, PARENT_ID, PARENT_NAME, MARKETING_NAME,
                null, null));
        assertFalse(contract.hasChanges(CONTRACT_NAME, PARENT_ID, PARENT_NAME, MARKETING_NAME,
                TOTAL_ENROLLMENT, MEDICARE_ELIGIBLE));
    }

    @Test
    void testUpdateAttestationCaseOne() {
        assertTrue(contract.updateAttestation(false, NOW_STRING));
    }

    @Test
    void testUpdateAttestationCaseTwo() {
        contract.setUpdateMode(Contract.UpdateMode.MANUAL);
        assertFalse(contract.updateAttestation(false, NOW_STRING));
    }

    @Test
    void testUpdateAttestationCaseThree() {
        contract.setAttestedOn(null);
        assertTrue(contract.updateAttestation(true, NOW_STRING));
    }

    @Test
    void testUpdateAttestationCaseFour() {
        contract.setAttestedOn(null);
        assertFalse(contract.updateAttestation(false, NOW_STRING));
    }

    @Test
    void testOther() {
        ContractDTO contractDTO = contract.toDTO();
        assertEquals(CONTRACT_NAME, contractDTO.getContractName());
        assertEquals(CONTRACT_NUMBER, contractDTO.getContractNumber());
        assertEquals(Contract.ContractType.NORMAL, contractDTO.getContractType());
        assertEquals(contractDTO.getId(), contract.getId());
        assertEquals(contractDTO.getAttestedOn(), contract.getAttestedOn());
        assertEquals(contractDTO.getTotalEnrollment(), contract.getTotalEnrollment());
        assertEquals(contractDTO.getMedicareEligible(), contract.getMedicareEligible());
    }

    @Test
    void testType() {
        assertFalse(contract.isTestContract());
        contract.setContractType(Contract.ContractType.CLASSIC_TEST);
        assertTrue(contract.isTestContract());
        contract.setContractType(Contract.ContractType.SYNTHEA);
        assertTrue(contract.isTestContract());

        assertTrue(contract.isAutoUpdatable());
        contract.setUpdateMode(Contract.UpdateMode.MANUAL);
        assertFalse(contract.isAutoUpdatable());
        contract.setUpdateMode(Contract.UpdateMode.NONE);
        assertFalse(contract.isAutoUpdatable());
    }

    @Test
    void testTime() {
        ZonedDateTime zonedDateTime = contract.getESTAttestationTime();
        assertEquals(0, zonedDateTime.toOffsetDateTime().toInstant().compareTo(NOW.toInstant()));
        System.out.println("Zoned  DateTime: " + zonedDateTime);
        System.out.println("Passed DateTime: " + NOW);
    }

    @Test
    void testAttestation() {
        assertTrue(contract.hasAttestation());
        contract.clearAttestation();
        assertFalse(contract.hasAttestation());
    }

    @Test
    void testUpdate() {
        contract.updateOrg(CONTRACT_NAME + "1",
                PARENT_ID + 1, PARENT_NAME + "1",
                MARKETING_NAME + "1", TOTAL_ENROLLMENT + 1,
                MEDICARE_ELIGIBLE + 1);

        assertEquals(CONTRACT_NAME + "1", contract.getContractName());
        assertEquals(PARENT_ID + 1, contract.getHpmsParentOrgId());
        assertEquals(PARENT_NAME + "1", contract.getHpmsParentOrg());
        assertEquals(TOTAL_ENROLLMENT + 1, contract.getTotalEnrollment());
        assertEquals(MEDICARE_ELIGIBLE + 1, contract.getMedicareEligible());
        assertEquals(NOW, contract.getAttestedOn());
        assertEquals(Contract.ContractType.NORMAL, contract.getContractType());
        assertEquals(0, contract.getESTAttestationTime().toOffsetDateTime().toInstant().compareTo(NOW.toInstant()));
        assertEquals(ID, contract.getId());
    }

    @Test
    void testConst() {
        Contract newContract = new Contract(CONTRACT_NUMBER, CONTRACT_NAME, PARENT_ID, PARENT_NAME,
                MARKETING_NAME, TOTAL_ENROLLMENT, MEDICARE_ELIGIBLE);
        assertEquals(newContract.getContractNumber(), contract.getContractNumber());
        assertEquals(newContract.getContractName(), contract.getContractName());
        assertEquals(newContract.getHpmsParentOrg(), contract.getHpmsParentOrg());
        assertEquals(newContract.getHpmsParentOrgId(), contract.getHpmsParentOrgId());
        assertEquals(newContract.getTotalEnrollment(), contract.getTotalEnrollment());
        assertEquals(newContract.getMedicareEligible(), contract.getMedicareEligible());
        assertNotEquals(newContract.getESTAttestationTime(), contract.getESTAttestationTime());
    }
}
