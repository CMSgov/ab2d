package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.Consent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class ConsentConverterServiceImpl implements ConsentConverterService {

    private static final Pattern HEALTH_INSURANCE_CLAIM_NUMBER_PATTERN = Pattern.compile("\\d{9}[A-Za-z0-9]{0,2}");

    private static final int HEALTH_INSURANCE_CLAIM_NUMBER_START = 0;
    private static final int HEALTH_INSURANCE_CLAIM_NUMBER_END = 11;
    private static final int EFFECTIVE_DATE_START = 354;
    private static final int EFFECTIVE_DATE_END = 362;      // ASK Denis - the column number seems to be same
    private static final int SOURCE_CODE_START = 362;       // ASK Denis - the column number seems to be same. Is that okay?
    private static final int SOURCE_CODE_END = 367;
    private static final int PREF_INDICATOR_START = 368;
    private static final int PREF_INDICATOR_END = 369;

    private static final String OPT_OUT_INDICATOR = "N";

    @Override
    public Optional<Consent> convert(String line, String filename, int lineNum) {
        if (isHeader(line) || isTrailer(line)) {
            // Ignore Header and Trailer rows
            return Optional.empty();
        }

        var sourceCode = line.substring(SOURCE_CODE_START, SOURCE_CODE_END);
        if (StringUtils.isBlank(sourceCode)) {
            return Optional.empty();
        }
        if (!sourceCode.trim().matches("1-?800")) {
            // If the source is blank, ignore this record
            throw new RuntimeException("Unexpected beneficiary data sharing source code" +  filename +  lineNum);
        }

        var prefIndicator = line.substring(PREF_INDICATOR_START, PREF_INDICATOR_END);
        if (!OPT_OUT_INDICATOR.equalsIgnoreCase(prefIndicator)) {
            // we only care about opt-out records
            return Optional.empty();
        }

        Consent consent = new Consent();
        consent.setPolicyCode("OPTOUT");
        consent.setPurposeCode("TREAT");
        consent.setLoIncCode("64292-6");
        consent.setScopeCode("patient-privacy");

        consent.setEffectiveDate(parseEffectiveDate(line, filename, lineNum));
        consent.setHicn(parseHealthInsuranceClaimNumber(line, filename, lineNum));

        return Optional.of(consent);
    }

    private boolean isHeader(String line) {
        return line.startsWith("HDR_BENEDATASHR");
    }

    private boolean isTrailer(String line) {
        return line.startsWith("TRL_BENEDATASHR");
    }

    /**
     * TODO : LocalDate vs OffsetDateTime - Check with Denis on how to proceed.
     * Either the column in the table needs to be changed to LocalDate
     * Or, we need to know the logic to convert the LocalDate into OffsetDate
     *
     * @param line
     * @param lineNum
     * @return
     */
    private OffsetDateTime parseEffectiveDate(String line, String filename, int lineNum) {
        var effectiveDateStr = line.substring(EFFECTIVE_DATE_START, EFFECTIVE_DATE_END);
        var effectiveDate = LocalDate.parse(effectiveDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            return OffsetDateTime.of(effectiveDate, LocalTime.of(0, 0), ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Cannot parse date from suppression record" +  filename +  lineNum);
        }
    }

    private String parseHealthInsuranceClaimNumber(String line, String filename, int lineNum) {
        var hicn = line.substring(HEALTH_INSURANCE_CLAIM_NUMBER_START, HEALTH_INSURANCE_CLAIM_NUMBER_END).trim();
        if (!HEALTH_INSURANCE_CLAIM_NUMBER_PATTERN.matcher(hicn).matches()) {
            throw new RuntimeException("HICN does not match expected format" +  filename +  lineNum);
        }
        return hicn;
    }
}
