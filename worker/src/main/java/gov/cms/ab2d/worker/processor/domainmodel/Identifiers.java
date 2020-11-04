package gov.cms.ab2d.worker.processor.domainmodel;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Identifiers {

    @EqualsAndHashCode.Include
    private final String beneficiaryId;

    private final String mbi;
}
