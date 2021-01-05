package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.Identifiers;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractMapping {
    private Set<Identifiers> patients;
    private int month;
}
