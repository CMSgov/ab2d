package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import java.time.OffsetDateTime;

import static javax.persistence.GenerationType.SEQUENCE;

@Entity
@Getter
@Setter
/**
 * Links sponsors to contracts bounded by an attestation date.
 */
public class Attestation {

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "attestation_id_seq")
    @SequenceGenerator(name = "attestation_id_seq", sequenceName = "attestation_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sponsor_id")
    private Sponsor sponsor;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime attestedOn;


}
