package gov.cms.ab2d.worker.processor.domainmodel;

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
