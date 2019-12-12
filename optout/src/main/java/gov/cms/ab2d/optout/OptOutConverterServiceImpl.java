package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOut;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Component
public class OptOutConverterServiceImpl implements OptOutConverterService {

    private static final Pattern HEALTH_INSURANCE_CLAIM_NUMBER_PATTERN = Pattern.compile("\\d{9}[A-Za-z0-9]{0,2}");

    private static final int HEALTH_INSURANCE_CLAIM_NUMBER_START = 0;
    private static final int HEALTH_INSURANCE_CLAIM_NUMBER_END = 11;
    private static final int EFFECTIVE_DATE_START = 354;
    private static final int EFFECTIVE_DATE_END = 362;
    private static final int SOURCE_CODE_START = 362;
    private static final int SOURCE_CODE_END = 367;
    private static final int PREF_INDICATOR_START = 368;
    private static final int PREF_INDICATOR_END = 369;

    private static final String OPT_OUT_INDICATOR = "N";

    @Override
    public Optional<OptOut> convert(String line) {
        if (isHeader(line)) {
            log.debug("Skipping Header row");
            return Optional.empty();
        }

        if (isTrailer(line)) {
            log.debug("Skipping Trailer row");
            return Optional.empty();
        }

        var sourceCode = line.substring(SOURCE_CODE_START, SOURCE_CODE_END);
        if (StringUtils.isBlank(sourceCode)) {
            log.debug("SourceCode is blank. Skipping row");
            return Optional.empty();
        }
        if (!sourceCode.trim().matches("1-?800")) {
            throw new RuntimeException("Invalid data sharing source code : " + sourceCode);
        }

        var prefIndicator = line.substring(PREF_INDICATOR_START, PREF_INDICATOR_END);
        if (!OPT_OUT_INDICATOR.equalsIgnoreCase(prefIndicator)) {
            // we only care about opt-out records
            log.debug("Preference Indicator is NOT opt-out. It was : {}, Skipping row", prefIndicator);
            return Optional.empty();
        }

        OptOut optOut = new OptOut();
        optOut.setPolicyCode("OPTOUT");
        optOut.setPurposeCode("TREAT");
        optOut.setLoIncCode("64292-6");
        optOut.setScopeCode("patient-privacy");

        optOut.setEffectiveDate(parseEffectiveDate(line));
        optOut.setHicn(parseHealthInsuranceClaimNumber(line));

        return Optional.of(optOut);
    }

    private boolean isHeader(String line) {
        return line.startsWith("HDR_BENEDATASHR");
    }

    private boolean isTrailer(String line) {
        return line.startsWith("TRL_BENEDATASHR");
    }

    /**
     * @param line
     * @return
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
     *
     * @param line
     * @return
     */
    private String parseHealthInsuranceClaimNumber(String line) {
        var claimNumber = line.substring(HEALTH_INSURANCE_CLAIM_NUMBER_START, HEALTH_INSURANCE_CLAIM_NUMBER_END).trim();
        if (!HEALTH_INSURANCE_CLAIM_NUMBER_PATTERN.matcher(claimNumber).matches()) {
            throw new RuntimeException("HICN does not match expected format");
        }
        return claimNumber;
    }
}
