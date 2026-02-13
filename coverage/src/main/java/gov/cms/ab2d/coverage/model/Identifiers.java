package gov.cms.ab2d.coverage.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashSet;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Identifiers {

    @EqualsAndHashCode.Include
    private final long beneficiaryId;

    private final String currentMbi;

    // LinkedHashSet maintains order of mbis as they are added
    // we always add the current mbi first
    private final LinkedHashSet<String> historicMbis;

    @EqualsAndHashCode.Include
    private final long patientIdV3;

    private final boolean isV3;

    private Identifiers(long beneficiaryId, String currentMbi, LinkedHashSet<String> historicMbis, long patientIdV3, boolean isV3) {
        this.beneficiaryId = beneficiaryId;
        this.currentMbi = currentMbi;
        this.historicMbis = historicMbis;
        this.patientIdV3 = patientIdV3;
        this.isV3 = isV3;
    }

    public Identifiers(long beneficiaryId, String currentMbi, LinkedHashSet<String> historicMbis) {
        this(beneficiaryId, currentMbi, historicMbis, -1L, false);
    }

    public static Identifiers ofV3(long patientIdV3, String currentMbi) {
        return new Identifiers(-1L, currentMbi, new LinkedHashSet<>(0), patientIdV3, true);
    }

    public boolean isV3() {
        return isV3;
    }
}
