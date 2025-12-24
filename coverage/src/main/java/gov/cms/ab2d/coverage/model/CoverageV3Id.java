package gov.cms.ab2d.coverage.model;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class CoverageV3Id implements Serializable {
    private final long patientId;
    private final String contract;
    private final int month;
    private final int year;
    private final String currentMbi;
}
