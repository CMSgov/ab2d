package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.model.Identifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SyntheticPatientIdCoverageSummary {

    static List<String> SyntheticParientIds = Arrays.asList("1S00E00AA59",
            "1S00E00AA28",
            "1S00E00AA91",
            "1S00E00AA69",
            "1S00E00AA73",
            "1S00E00AA55",
            "1S00E00AA84",
            "1S00E00AA49",
            "1S00E00AA22",
            "1S00E00AA82");

    public static CoverageSummary getSyntheticCoverageSummary(CoverageSummary originalCoverageSummary) {
        Random random = new Random();
        String id = SyntheticParientIds.get(random.nextInt(SyntheticParientIds.size()));

        Identifiers originalIdentifiers = originalCoverageSummary.getIdentifiers();
        Identifiers identifiers = new Identifiers(originalIdentifiers.getBeneficiaryId(),
                originalIdentifiers.getCurrentMbi(), id, originalIdentifiers.getHistoricMbis());

        return new CoverageSummary(identifiers, originalCoverageSummary.getContract(), originalCoverageSummary.getDateRanges());
    }


}
