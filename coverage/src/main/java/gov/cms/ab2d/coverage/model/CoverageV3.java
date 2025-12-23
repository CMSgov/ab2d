package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.model.TimestampBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Entity
@Table(name = "coverage_v3", schema = "v3")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CoverageV3 extends TimestampBase {

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
