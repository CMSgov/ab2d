package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.model.TimestampBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "coverage_v3_historical", schema = "v3")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CoverageV3Historical extends TimestampBase {

    @NotNull
    @EqualsAndHashCode.Include
    @Id
    private long patientId;

    @NotNull
    @EqualsAndHashCode.Include
    private String contract;

    @Column
    @EqualsAndHashCode.Include
    private int month;

    @Column
    @EqualsAndHashCode.Include
    private int year;

    @Column
    @EqualsAndHashCode.Include
    private String currentMbi;

    @Column
    @EqualsAndHashCode.Include
    private String historicMbis; // TODO do we need this?
}
