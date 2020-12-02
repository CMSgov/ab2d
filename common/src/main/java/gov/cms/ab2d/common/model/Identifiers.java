package gov.cms.ab2d.common.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.LinkedHashSet;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Identifiers {

    @EqualsAndHashCode.Include
    private final String beneficiaryId;

    private final String currentMbi;

    // LinkedHashSet maintains order of mbis as they are added
    // we always add the current mbi first
    private final LinkedHashSet<String> historicMbis;
}
