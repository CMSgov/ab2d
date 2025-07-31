package gov.cms.ab2d.fetcher.model;

import gov.cms.ab2d.filter.FilterOutByDate;
import lombok.Getter;

import java.util.List;

/**
 * Information about a patient and it's coverage information. It includes all its ids (bene, current and
 * historical mbis) as well as the time ranges the beneficiary was coveraged by the PDP
 */
@Getter
public class PatientCoverage {
    private long beneId;
    private String currentMBI;
    private String[] historicMBIs;
    private List<FilterOutByDate.DateRange> dateRanges;
}
