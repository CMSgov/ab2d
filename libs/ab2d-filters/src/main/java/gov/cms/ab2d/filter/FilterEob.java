package gov.cms.ab2d.filter;

import org.hl7.fhir.instance.model.api.IBaseResource;

import lombok.experimental.UtilityClass;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class FilterEob {

    /**
     * Does all the filtering. If null is returned
     * @param resource - the resource to check
     * @param dateRanges - the valid date ranges the beneficiary was a member
     * @param earliestDate - the earliest date after which the billable period must be after
     * @param attTime - the time the contract was attested
     * @param skipBillablePeriodCheck - if you want to turn off date checking (used with testing data)
     * @return an optional of the resource. If filtered out, isPresent() is false
     */
    public static Optional<IBaseResource> filter(IBaseResource resource, List<FilterOutByDate.DateRange> dateRanges,
                                                 Date earliestDate, Date attTime, boolean skipBillablePeriodCheck) {
        // If there is no attestation date, cannot return the data
        if (attTime == null) {
            return Optional.empty();
        }
        // Ignore Part D
        if (EobUtils.isPartD(resource)) {
            return Optional.empty();
        }
        if (skipBillablePeriodCheck || FilterOutByDate.valid(resource, attTime, earliestDate, dateRanges)) {
            return Optional.of(resource);
        }
        return Optional.empty();
    }
}
