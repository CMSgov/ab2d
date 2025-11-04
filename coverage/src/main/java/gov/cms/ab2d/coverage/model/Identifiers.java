package gov.cms.ab2d.coverage.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.LinkedHashSet;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Identifiers {

    @EqualsAndHashCode.Include
    private final long beneficiaryId;

    private final String currentMbi;

    private final String patientIdV3;

    // LinkedHashSet maintains order of mbis as they are added
    // we always add the current mbi first
    private final LinkedHashSet<String> historicMbis;

    public Identifiers(long beneficiaryId, String currentMbi, LinkedHashSet<String> historicMbis) {
        this(beneficiaryId, currentMbi, null, historicMbis);
    }

}
