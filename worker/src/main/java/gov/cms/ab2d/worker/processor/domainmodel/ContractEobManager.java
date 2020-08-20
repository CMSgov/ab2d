package gov.cms.ab2d.worker.processor.domainmodel;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.ab2d.common.util.FHIRUtil;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.StreamHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ContractEobManager {
    public enum ResourceStatus {
        INVALID,
        VALID,
        UNKNOWN
    }
    static final List<Integer> ALL_MONTHS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

    private final FhirContext fhirContext;

    private final OffsetDateTime attTime;

    private final Map<String, EobSearchResponse> unknownEobs = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, EobSearchResponse> validEobs = Collections.synchronizedMap(new HashMap<>());

    private final boolean skipBillablePeriodCheck;
    private final Date earliestDate;

    public ContractEobManager(FhirContext fhirContext, boolean skipBillablePeriodCheck, Date earliestDate, OffsetDateTime attTime) {
        this.fhirContext = fhirContext;
        this.skipBillablePeriodCheck = skipBillablePeriodCheck;
        this.earliestDate = earliestDate;
        this.attTime = attTime;
    }

    public void writeValidEobs(StreamHelper helper) throws IOException {
        IParser jsonParser = fhirContext.newJsonParser();
        String payload = "";
        int count = 0;
        try {
            for (Map.Entry<String, EobSearchResponse> response : validEobs.entrySet()) {
                Iterator<Resource> resources = response.getValue().getResources().iterator();
                while (resources.hasNext()) {
                    Resource r = resources.next();
                    payload = jsonParser.encodeResourceToString(r) + System.lineSeparator();
                    count++;
                    helper.addData(payload.getBytes(StandardCharsets.UTF_8));
                    resources.remove();
                }
            }
        } catch (Exception e) {
                handleException(helper, payload, e);
        }
        log.debug("finished writing [{}] resources", count);
    }

    public void addResources(EobSearchResponse response) {
        String patientId = response.getPatient().getPatientId();
        if (unknownEobs.get(patientId) == null) {
            unknownEobs.put(patientId, response);
        } else {
            List<Resource> resources = new ArrayList<>(unknownEobs.get(patientId).getResources());
            resources.addAll(response.getResources());
            EobSearchResponse newResponse = new EobSearchResponse(response.getPatient(), resources);
            unknownEobs.put(patientId, newResponse);
        }
        if (validEobs.get(patientId) == null) {
            validEobs.put(patientId, new EobSearchResponse(response.getPatient(), new ArrayList<>()));
        }
    }

    public void validateResources(ContractBeneficiaries.PatientDTO patient) {
        for (Map.Entry<String, EobSearchResponse> entry : unknownEobs.entrySet()) {
            List<Resource> resources = entry.getValue().getResources();
            entry.getValue().setResources(resources.stream().filter(r -> !updateData(r, patient)).collect(Collectors.toList()));
        }
    }

    private boolean updateData(Resource resource, ContractBeneficiaries.PatientDTO patient) {
        ResourceStatus validStatus = validResource(resource, patient);
        switch (validStatus) {
            // We know that the EOB is in the right time for a patient in the right date range
            case VALID:
                validEobs.get(patient.getPatientId()).getResources().add(resource);
                return true;
            case INVALID:
            case UNKNOWN:
            default:
                return false;
        }
    }

    private ResourceStatus validResource(Resource resource, ContractBeneficiaries.PatientDTO patient) {
        ResourceStatus status = ResourceStatus.UNKNOWN;

        // The resource doesn't exist, it' not valid
        if (resource == null) {
            return ResourceStatus.INVALID;
        }

        // If we've found the patient in the contract, it's valid, otherwise, we may not yet know (all patients have not been returned)
        if (validPatientInContract((ExplanationOfBenefit) resource, patient)) {
            status = ResourceStatus.VALID;
        }

        // Check against dates (if we're checking it)
        if (skipBillablePeriodCheck) {
            status = ResourceStatus.VALID;
        } else {
            if (!FilterOutByDate.valid((ExplanationOfBenefit) resource, attTime, earliestDate, patient.getDateRangesUnderContract())) {
                status = ResourceStatus.INVALID;
            }
        }

        return status;
    }

    void handleException(StreamHelper helper, String data, Exception e) throws IOException {
        String errMsg = ExceptionUtils.getRootCauseMessage(e);
        OperationOutcome operationOutcome = FHIRUtil.getErrorOutcome(errMsg);
        IParser jsonParser = fhirContext.newJsonParser();
        String payload = jsonParser.encodeResourceToString(operationOutcome) + System.lineSeparator();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        helper.addError(data);
    }

    /**
     * returns true if the patient is a valid member of a contract, false otherwise. If either value is empty,
     * it returns false
     *
     * @param benefit - The benefit to check
     * @param patient - the patient who was used to create the query
     * @return true if this patient is a member of the correct contract
     */
    boolean validPatientInContract(ExplanationOfBenefit benefit, ContractBeneficiaries.PatientDTO patient) {
        if (benefit == null || patient == null) {
            log.debug("Passed an invalid benefit or an invalid list of patients");
            return false;
        }
        String patientId = benefit.getPatient().getReference();
        if (patientId == null) {
            return false;
        }
        patientId = patientId.replaceFirst("Patient/", "");
        return patientId.equalsIgnoreCase(patient.getPatientId());
    }

    Map<String, EobSearchResponse> getUnknownEobs() {
        return unknownEobs;
    }

    Map<String, EobSearchResponse> getValidEobs() {
        return validEobs;
    }

    public void cleanUpKnownInvalidPatients(List<Integer> monthsDone) {
        for (Map.Entry<String, EobSearchResponse> entry: unknownEobs.entrySet()) {
            EobSearchResponse response = entry.getValue();
            List<Resource> resources = response.getResources();
            response.setResources(resources.parallelStream()
                    .filter(r -> !covered(r, monthsDone))
                    .collect(Collectors.toList()));
        }
    }

    static boolean covered(Resource resource, List<Integer> monthsDone) {
        ExplanationOfBenefit eob = (ExplanationOfBenefit) resource;
        List<Integer> coveredMonths = getCoveredMonths(eob.getBillablePeriod());
        for (Integer coveredMonth : coveredMonths) {
            if (monthsDone.contains(coveredMonth)) {
                return true;
            }
        }
        return false;
    }

    static List<Integer> getCoveredMonths(Period period) {
        Date startDate = period.getStart();
        int startMonth = getMonth(startDate);
        int startYear = getYear(startDate);
        Date endDate = period.getEnd();
        int endMonth = getMonth(endDate);
        int endYear = getYear(endDate);
        List<Integer> results = new ArrayList<>();
        if (startYear != endYear) {
            if (endYear - startYear > 1 || endMonth > startMonth) {
                return ALL_MONTHS;
            } else {
                results = new ArrayList<>();
                for (int i = startMonth; i <= 12; i++) {
                    results.add(i);
                }
                for (int i = 1; i <= endMonth; i++) {
                    results.add(i);
                }
                return results;
            }
        } else {
            for (int i = startMonth; i <= endMonth; i++) {
                results.add(i);
            }
        }
        return results;
    }

    static int getMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.MONTH) + 1;
    }

    static int getYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }
}
