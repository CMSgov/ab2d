package gov.cms.ab2d.coverage.model.v3;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode
public class CoverageV3Id implements Serializable {
    private long patientId;
    private String contract;
    private int month;
    private int year;
    private String currentMbi;
}
