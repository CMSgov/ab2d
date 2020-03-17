package gov.cms.ab2d.optout;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.OptOut;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OptOutConverterServiceImpl implements OptOutConverterService {
    @Autowired
    private BFDClient bfdClient;

    private static final Pattern HICN_PATTERN = Pattern.compile("\\d{9}[A-Za-z0-9]{0,2}");

    private static final int HICN_START = 0;
    private static final int HICN_END = 11;
    private static final int EFFECTIVE_DATE_START = 354;
    private static final int EFFECTIVE_DATE_END = 362;
    private static final int SOURCE_CODE_START = 362;
    private static final int SOURCE_CODE_END = 367;
    private static final int PREF_INDICATOR_START = 368;
    private static final int PREF_INDICATOR_END = 369;

    private static final String OPT_OUT_INDICATOR = "N";

    /**
     * Convert a line to a list of OptOut values. Over 99% of the time, this list will contain 1 OptOut object.
     * In the rare occasion where the HICN resolves to more than 1 patient, the list will contain all of those
     * patient objects.
     *
     * @param line - line in the file
     *
     * @return the list of OptOut objects that match the HICN value
     */
    @Override
    public List<OptOut> convert(String line) {
        List<OptOut> optOuts = new ArrayList<>();
        if (isHeader(line)) {
            log.debug("Skipping Header row");
            return optOuts;
        }

        if (isTrailer(line)) {
            log.debug("Skipping Trailer row");
            return optOuts;
        }

        var sourceCode = line.substring(SOURCE_CODE_START, SOURCE_CODE_END);
        if (StringUtils.isBlank(sourceCode)) {
            log.debug("SourceCode is blank. Skipping row");
            return optOuts;
        }
        if (!sourceCode.trim().matches("1-?800")) {
            throw new RuntimeException("Invalid data sharing source code : " + sourceCode);
        }

        var prefIndicator = line.substring(PREF_INDICATOR_START, PREF_INDICATOR_END);
        if (!OPT_OUT_INDICATOR.equalsIgnoreCase(prefIndicator)) {
            // we only care about opt-out records
            log.debug("Preference Indicator is NOT opt-out. It was : {}, Skipping row", prefIndicator);
            return optOuts;
        }

        // Find the HICN ID
        var hicn = parseHICN(line);
        optOuts.addAll(createOptOuts(line, hicn));

        return optOuts;
    }

    /**
     * From BB, receive the list of patients with that HICN Id and create OptOut objects
     * @param line - line in the file
     * @param hicn - The HICN Id
     * @return a list of OptOut records
     */
    private List<OptOut> createOptOuts(String line, String hicn) {
        return getPatientInfo(hicn).stream()
                .map(patient -> createOptOut(line, hicn, patient))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve the list of patients with an HICN Id from BB. The will be contained within a Bundle which will have
     * a list of Resources (of type Patient) that match the ID
     *
     * @param hicn - The HICN Id
     * @return - the list of Patient Resource objects with that ID
     */
    private List<Patient> getPatientInfo(String hicn) {
        Bundle bundle = bfdClient.requestPatientFromServer(hicn);
        if (bundle != null) {
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
            return entries.stream()
                    // Get the resource
                    .map(Bundle.BundleEntryComponent::getResource)
                    // Get only the Patients (although that's all that should be present
                    .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                    .map(resource -> (Patient) resource)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * @param line
     * @param hicn
     * @param patient
     * @return an OptOut record
     */
    private OptOut createOptOut(String line, String hicn, Patient patient) {
        OptOut optOut = new OptOut();
        optOut.setPolicyCode("OPTOUT");
        optOut.setPurposeCode("TREAT");
        optOut.setLoIncCode("64292-6");
        optOut.setScopeCode("patient-privacy");
        optOut.setEffectiveDate(parseEffectiveDate(line));
        optOut.setHicn(hicn);
        optOut.setCcwId(getCcwId(patient));
        optOut.setMbi(getMbi(patient));
        return optOut;
    }

    /**
     * From the Patient object, find the id that is the Beneficiary Id
     *
     * @param patient - the patient object
     * @return the Beneficiary Id
     */
    private static String getCcwId(Patient patient) {
        return getIdVal(patient, "bene_id");
    }

    /**
     * From the Patient object, find the Id that is the MBI Id
     *
     * @param patient - the patient object
     * @return the MBI Id
     */
    private static String getMbi(Patient patient) {
        return getIdVal(patient, "us-mbi");
    }

    /**
     * Convenience method for retrieving a type of identifier code. It system variable, describing the type
     * of id you want needs only be the end part of the value of enough length that it is identifiable. It doesn't
     * have to be the entire URL
     *
     * @param patient - the patient object
     * @param system - the type of id you are looking for
     * @return - the id value
     */
    private static String getIdVal(Patient patient, String system) {
        if (patient == null || system == null) {
            return null;
        }
        return patient.getIdentifier().stream()
                .filter(c -> c.getSystem().toLowerCase().endsWith(system.toLowerCase())).map(Identifier::getValue)
                .findFirst().orElse(null);
    }

    /**
     * If the line is a header line
     *
     * @param line - the line to check for header information
     * @return true if it is a header line
     */
    private boolean isHeader(String line) {
        return line.startsWith("HDR_BENEDATASHR");
    }

    /**
     * If the line is a trailer line
     *
     * @param line - the line to check for trailer information
     * @return true if it is a trailer line
     */
    private boolean isTrailer(String line) {
        return line.startsWith("TRL_BENEDATASHR");
    }

    /**
     * Parse the effective date from a string in the file
     *
     * @param line - the line containing the date
     * @return the date as a LocalDate (day only)
     */
    private LocalDate parseEffectiveDate(String line) {
        var effectiveDateStr = line.substring(EFFECTIVE_DATE_START, EFFECTIVE_DATE_END);
        try {
            return LocalDate.parse(effectiveDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid Date : " + effectiveDateStr, e);
        }
    }

    /**
     * Parse the HICN id from the line
     *
     * @param line - the line containing the HICN
     * @return the value of the HICN Id
     */
    private String parseHICN(String line) {
        var claimNumber = line.substring(HICN_START, HICN_END).trim();
        if (!HICN_PATTERN.matcher(claimNumber).matches()) {
            throw new RuntimeException("HICN does not match expected format");
        }
        return claimNumber;
    }
}
