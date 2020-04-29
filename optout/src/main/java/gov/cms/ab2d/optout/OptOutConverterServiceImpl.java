package gov.cms.ab2d.optout;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.OptOut;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

    private static final Pattern PRE_1964_PATTERN = Pattern.compile("[a-zA-Z]\\d{6}");

    private static final Pattern POST_1964_PATTERN = Pattern.compile("[a-zA-Z]\\d{9}");

    private static final int HICN_START = 0;
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

        // Find the identifier
        var identifier = parseIdentifier(line);
        optOuts.addAll(createOptOuts(line, identifier));

        return optOuts;
    }

    /**
     * From BB, receive the list of patients with that HICN or MBI Id and create OptOut objects
     * @param line - line in the file
     * @param patientId - The HICN/MBI Id
     * @return a list of OptOut records
     */
    private List<OptOut> createOptOuts(String line, String patientId) {
        Pair<List<Patient>, Boolean> patientData = getPatientInfo(patientId);
        List<OptOut> optOuts = new ArrayList<>();
        for (Patient patient : patientData.getLeft()) {
            optOuts.add(createOptOut(line, patientId, patient, patientData.getRight()));
        }

        return optOuts;
    }

    private boolean isHicn(String patientId) {
        return PRE_1964_PATTERN.matcher(patientId).matches()
            || POST_1964_PATTERN.matcher(patientId).matches()
            || HICN_PATTERN.matcher(patientId).matches();
    }

    /**
     * Retrieve the list of patients with an patient Id that is in either a HICN or MBI format from BB. The will be
     * contained within a Bundle which will have a list of Resources (of type Patient) that match the ID
     *
     * @param patientId - The patient Id
     * @return - the list of Patient Resource objects with that ID
     */
    private Pair<List<Patient>, Boolean> getPatientInfo(String patientId) {
        Bundle bundle;
        boolean isHicn;
        if (isHicn(patientId)) {
            bundle = bfdClient.requestPatientByHICN(patientId);
            isHicn = true;
        } else {
            bundle = bfdClient.requestPatientByMBI(patientId);
            isHicn = false;
        }
        if (bundle != null) {
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
            return Pair.of(entries.stream()
                    // Get the resource
                    .map(Bundle.BundleEntryComponent::getResource)
                    // Get only the Patients (although that's all that should be present
                    .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                    .map(resource -> (Patient) resource)
                    .collect(Collectors.toList()), isHicn);
        }
        return Pair.of(new ArrayList<>(), isHicn);
    }

    /**
     * @param line
     * @param identifier
     * @param patient
     * @return an OptOut record
     */
    private OptOut createOptOut(String line, String identifier, Patient patient, boolean isHicn) {
        OptOut optOut = new OptOut();
        optOut.setPolicyCode("OPTOUT");
        optOut.setPurposeCode("TREAT");
        optOut.setLoIncCode("64292-6");
        optOut.setScopeCode("patient-privacy");
        optOut.setEffectiveDate(parseEffectiveDate(line));
        if (isHicn) {
            optOut.setHicn(identifier);
        }
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
     * Parse the id from the line, which could be a HICN or MBI
     *
     * @param line - the line containing the identifier
     * @return the value of the Id
     */
    private String parseIdentifier(String line) {
        int firstSpace = line.indexOf(" ");
        var claimNumber = line.substring(HICN_START, firstSpace).trim();

        return claimNumber;
    }
}
