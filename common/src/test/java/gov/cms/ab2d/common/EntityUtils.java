package gov.cms.ab2d.common;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Random;

public class EntityUtils {

    public static final Random RANDOM = new Random();

    public static String createBeneId() {
        return "patientId_" + Instant.now().getNano();
    }

    public static CoveragePeriod createCoveragePeriod(CoveragePeriodRepository cpr,
                                                      Contract contract, int month, int year) {
        CoveragePeriod coveragePeriod = new CoveragePeriod();
        coveragePeriod.setContract(contract);
        coveragePeriod.setMonth(month);
        coveragePeriod.setYear(year);

        return cpr.save(coveragePeriod);
    }

    public static CoverageSearchEvent createCoverageSearchEvent(CoverageSearchEventRepository cser,
                                                                CoveragePeriod coveragePeriod, String description) {
        CoverageSearchEvent coverageSearchEvent = new CoverageSearchEvent();
        coverageSearchEvent.setCoveragePeriod(coveragePeriod);
        coverageSearchEvent.setNewStatus(JobStatus.SUBMITTED);
        coverageSearchEvent.setDescription(description);

        return cser.save(coverageSearchEvent);
    }

    public static Coverage createCoverage(CoverageRepository cr, CoveragePeriod coveragePeriod, CoverageSearchEvent coverageSearchEvent, String bene) {
        Coverage coverage = new Coverage();
        coverage.setCoveragePeriod(coveragePeriod);
        coverage.setCoverageSearchEvent(coverageSearchEvent);
        coverage.setBeneficiaryId(bene);
        return cr.save(coverage);
    }

    public static Contract createContract(ContractRepository cr, Sponsor sponsor, final String contractNumber) {
        Contract contract = new Contract();
        contract.setContractName(contractNumber);
        contract.setContractNumber(contractNumber);
        contract.setAttestedOn(OffsetDateTime.now().minusDays(10));
        contract.setSponsor(sponsor);

        sponsor.getContracts().add(contract);
        return cr.save(contract);
    }

    public static Sponsor createSponsor(SponsorRepository sr) {
        Sponsor parent = new Sponsor();
        parent.setOrgName("Parent");
        parent.setLegalName("Parent");
        parent.setHpmsId(350);

        Sponsor sponsor = new Sponsor();
        sponsor.setOrgName("Hogwarts School of Wizardry");
        sponsor.setLegalName("Hogwarts School of Wizardry LLC");

        sponsor.setHpmsId(RANDOM.nextInt());
        sponsor.setParent(parent);
        parent.getChildren().add(sponsor);
        return sr.save(sponsor);
    }
}
