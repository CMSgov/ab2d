package gov.cms.ab2d.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
/**
 * Links sponsors to contracts bounded by an attestation date.
 */
public class Attestation {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sponsor_id")
    private Sponsor sponsor;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    private Contract contract;

    private OffsetDateTime attestationDate;


}
