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
import org.hl7.fhir.dstu3.model.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
public class ContractEobManager {
    public enum ResourceStatus {
        INVALID,
        VALID,
        UNKNOWN
    }

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
        Iterator<Map.Entry<String, EobSearchResponse>> resourceIterable = validEobs.entrySet().iterator();
        String payload = "";
        int count = 0;
        while (resourceIterable.hasNext()) {
            try {
                Map.Entry<String, EobSearchResponse> resource = resourceIterable.next();
                for (Resource r : resource.getValue().getResources()) {
                    payload = jsonParser.encodeResourceToString(r) + System.lineSeparator();
                    count++;
                    helper.addData(payload.getBytes(StandardCharsets.UTF_8));
                }
                resourceIterable.remove();
            } catch (Exception e) {
                // log.warn("Encountered exception while processing job resources: {}", e.getMessage());
                handleException(helper, payload, e);
            }
        }
        log.debug("finished writing [{}] resources", count);
    }

    public void addResources(EobSearchResponse response) {
        unknownEobs.put(response.getPatient().getPatientId(), response);
        validEobs.put(response.getPatient().getPatientId(), new EobSearchResponse(response.getPatient(), new ArrayList<>()));
    }

    public void validateResources() {
        for (Map.Entry<String, EobSearchResponse> entry : unknownEobs.entrySet()) {
            String patientId = entry.getKey();
            EobSearchResponse eobSearchResponse = entry.getValue();
            Iterator<Resource> resourceIter = eobSearchResponse.getResources().iterator();
            while (resourceIter.hasNext()) {
                Resource r = resourceIter.next();
                ResourceStatus validStatus = validResource(r, eobSearchResponse.getPatient());
                switch (validStatus) {
                    // We know that the EOB is in the right time for a patient in the right date range
                    case VALID:
                        validEobs.get(patientId).getResources().add(r);
                        resourceIter.remove();
                        break;
                    case INVALID:
                        resourceIter.remove();
                        break;
                    case UNKNOWN:
                    default:
                        break;
                }
            }
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
        }
        if (!FilterOutByDate.valid((ExplanationOfBenefit) resource, attTime, earliestDate, patient.getDateRangesUnderContract())) {
            status = ResourceStatus.INVALID;
        }

        return status;
    }

    private void handleException(StreamHelper helper, String data, Exception e) throws IOException {
        var errMsg = ExceptionUtils.getRootCauseMessage(e);
        var operationOutcome = FHIRUtil.getErrorOutcome(errMsg);

        var jsonParser = fhirContext.newJsonParser();
        var payload = jsonParser.encodeResourceToString(operationOutcome) + System.lineSeparator();

        var byteArrayOutputStream = new ByteArrayOutputStream();
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
        return (patientId.equalsIgnoreCase(patient.getPatientId()));
    }
}
