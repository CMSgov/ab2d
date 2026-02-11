package gov.cms.ab2d.coverage.model.v3;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "coverage_v3_historical", schema = "v3")
@IdClass(CoverageV3Id.class)
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class CoverageV3Historical {

    @NotNull
    @EqualsAndHashCode.Include
    @Id
    @Column(name="patient_id")
    private long patientId;

    @NotNull
    @EqualsAndHashCode.Include
    @Id
    private String contract;

    @Column
    @EqualsAndHashCode.Include
    @Id
    private int month;

    @Column
    @EqualsAndHashCode.Include
    @Id
    private int year;

    @Column(name="current_mbi")
    @EqualsAndHashCode.Include
    @Id
    private String currentMbi;

    @Column(name="historic_mbis")
    @EqualsAndHashCode.Include
    private String historicMbis;
}
