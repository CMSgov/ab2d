package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.cms.ab2d.fhir.EobUtils.getJustId;
import static gov.cms.ab2d.fhir.PatientIdentifier.CURRENT_MBI;
import static gov.cms.ab2d.fhir.PatientIdentifier.HISTORIC_MBI;
import static gov.cms.ab2d.fhir.PatientIdentifier.MBI_ID_R4;

/**
 * Util methods to manipulate identifiers for different FHIR versions
 */
@Slf4j
public final class IdentifierUtils {
    public static final String CURRENCY_IDENTIFIER =
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency";
    public static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";

    private IdentifierUtils() {
    }

    /**
     * Get all the identifiers in the Patient Resource
     *
     * @param patient - the patient resource
     * @return a list of identifiers
     */
    public static List<PatientIdentifier> getIdentifiers(IDomainResource patient) {
        if (patient == null) {
            return null;
        }
        List<PatientIdentifier> derivedIds = new ArrayList<>();
        String idVal = patient.getId();
        if (idVal != null && !idVal.isEmpty()) {
            PatientIdentifier beneId = new PatientIdentifier();
            beneId.setType(PatientIdentifier.Type.BENE_ID);
            beneId.setCurrency(PatientIdentifier.Currency.CURRENT);
            beneId.setValue(getJustId(idVal));
            derivedIds.add(beneId);
        }

        List identifiers = (List) Versions.invokeGetMethod(patient, "getIdentifier");
        derivedIds.addAll((List<PatientIdentifier>) identifiers.stream()
                .map(c -> getIdentifier((ICompositeType) c))
                .filter(c -> c != null)
                .collect(Collectors.toList()));
        return derivedIds;
    }

    /**
     * Return the beneficiary id for a patient
     *
     * @param ids - all identifiers
     * @return the Identifier
     */
    public static PatientIdentifier getBeneId(List<PatientIdentifier> ids) {
        if (ids == null) {
            return null;
        }
        return ids.stream().filter(c -> c.getType() == PatientIdentifier.Type.BENE_ID).findFirst().orElse(null);
    }

    /**
     * Retrieve our Identifier object from a FHIR Identifier object
     *
     * @param id - the FHIR Identifier
     * @return - our standardized identifier
     */
    public static PatientIdentifier getIdentifier(ICompositeType id) {
        String system = (String) Versions.invokeGetMethod(id, "getSystem");
        if (system == null) {
            return null;
        }
        PatientIdentifier derivedId = new PatientIdentifier();
        String value = (String) Versions.invokeGetMethod(id, "getValue");
        derivedId.setValue(value);
        derivedId.setType(PatientIdentifier.Type.fromSystem(system));
        derivedId.setCurrency(getCurrency(id));
        return derivedId;
    }

    /**
     * Return the current MBI from the Patient resource. First it looks for an MBI that is marked as current but
     * it will also return one with an unknown currency
     *
     * @param identifiers - the list of patient identifiers
     * @return the most relevant MBI Identifier or null if it could not find a relevant identifier
     */
    public static PatientIdentifier getCurrentMbi(List<PatientIdentifier> identifiers) {
        if (identifiers == null) {
            return null;
        }
        List<PatientIdentifier> mbis = identifiers.stream()
                .filter(Objects::nonNull)
                .filter(c -> c.getType() == PatientIdentifier.Type.MBI || c.getType() == PatientIdentifier.Type.MBI_R4)
                .collect(Collectors.toList());

        if (mbis.isEmpty()) {
            return null;
        }
        PatientIdentifier id = mbis.stream()
                .filter(c -> c.getCurrency() == PatientIdentifier.Currency.CURRENT)
                .findFirst()
                .orElse(null);

        if (id != null) {
            return id;
        }
        return mbis.stream()
                .filter(c -> c.getCurrency() == PatientIdentifier.Currency.UNKNOWN)
                .findFirst()
                .orElse(null);
    }

    /**
     * Return the historical MBIs from the Patient resource
     *
     * @param ids - the patient identifiers
     * @return the set of historical MBIs
     */
    public static LinkedHashSet<PatientIdentifier> getHistoricMbi(List<PatientIdentifier> ids) {
        if (ids == null) {
            return null;
        }
        Set<PatientIdentifier> historic = ids.stream()
                .filter(c -> c.getCurrency() == PatientIdentifier.Currency.HISTORIC)
                .collect(Collectors.toSet());
        return new LinkedHashSet<>(historic);
    }

    /**
     * Return the system from the identifier
     *
     * @param identifier - the identifier
     * @return the value of the system
     */
    private static String getSystem(ICompositeType identifier) {
        return (String) Versions.invokeGetMethod(identifier, "getSystem");
    }

    /**
     * Return the value from the identifier
     *
     * @param identifier - the identifier
     * @return the value of the identifier
     */
    private static String getValue(ICompositeType identifier) {
        return (String) Versions.invokeGetMethod(identifier, "getValue");
    }

    /**
     * From a FHIR identifier include MBI information, get the currency information
     *
     * @param identifier - the identifier
     * @return - the currency information
     */
    private static PatientIdentifier.Currency getCurrencyMbi(ICompositeType identifier) {
        Optional<IBaseExtension> currencyExtension = getCurrencyExtension(identifier);
        if (currencyExtension.isEmpty()) {
            return PatientIdentifier.Currency.UNKNOWN;
        }
        Object code = Versions.invokeGetMethod(currencyExtension.get(), "getValue");
        String codeValue = (String) Versions.invokeGetMethod(code, "getCode");
        if (codeValue == null) {
            return PatientIdentifier.Currency.UNKNOWN;
        }
        switch (codeValue) {
            case HISTORIC_MBI:
                return PatientIdentifier.Currency.HISTORIC;
            case CURRENT_MBI:
                return PatientIdentifier.Currency.CURRENT;
            default:
                return PatientIdentifier.Currency.UNKNOWN;
        }
    }

    /**
     * Get the currency information for CARIN standard MBI identifier
     *
     * @param identifier - the FHIR identifier
     * @return the currency information
     */
    private static PatientIdentifier.Currency getCurrencyMbiStandard(ICompositeType identifier) {
        PatientIdentifier.Currency currency = getCurrencyFromPeriod(identifier);
        if (currency != PatientIdentifier.Currency.UNKNOWN) {
            return currency;
        }
        return getCurrencyFromTypeCodingExtension(identifier);
    }

    private static PatientIdentifier.Currency getCurrencyFromTypeCodingExtension(ICompositeType identifier) {
        Object type = Versions.invokeGetMethod(identifier, "getType");
        if (type == null) {
            return PatientIdentifier.Currency.UNKNOWN;
        }
        List vals = (List) Versions.invokeGetMethod(type, "getCoding");
        if (vals == null || vals.isEmpty()) {
            return PatientIdentifier.Currency.UNKNOWN;
        }
        Object val = vals.get(0);
        String codeSystem = (String) Versions.invokeGetMethod(val, "getSystem");
        String codeValue = (String) Versions.invokeGetMethod(val, "getCode");
        if (codeSystem != null && codeSystem.equalsIgnoreCase(MBI_ID_R4) && ("MB".equalsIgnoreCase(codeValue) || "MC".equalsIgnoreCase(codeValue))) {
            List extensions = (List) Versions.invokeGetMethod(val, "getExtension");
            if (extensions != null && extensions.size() > 0) {
                Object extension = extensions.get(0);
                String url = (String) Versions.invokeGetMethod(extension, "getUrl");
                if (url != null && url.equalsIgnoreCase(CURRENCY_IDENTIFIER)) {
                    Object extValue = Versions.invokeGetMethod(extension, "getValue");
                    String extValueSystem = (String) Versions.invokeGetMethod(extValue, "getSystem");
                    if (CURRENCY_IDENTIFIER.equalsIgnoreCase(extValueSystem)) {
                        String extValueCode = (String) Versions.invokeGetMethod(extValue, "getCode");
                        if (CURRENT_MBI.equalsIgnoreCase(extValueCode)) {
                            return PatientIdentifier.Currency.CURRENT;
                        }
                        if (HISTORIC_MBI.equalsIgnoreCase(extValueCode)) {
                            return PatientIdentifier.Currency.HISTORIC;
                        }
                    }
                }
            }
        }
        return PatientIdentifier.Currency.UNKNOWN;
    }

    /**
     * If the period has a value for the identifier, return the information from that, otherwise UNKNOWN
     *
     * @param identifier - the Identifier
     * @return the currency or UNKNOWN if there are no values
     */
    public static PatientIdentifier.Currency getCurrencyFromPeriod(ICompositeType identifier) {
        Object period = Versions.invokeGetMethod(identifier, "getPeriod");
        Date start = null;
        Date end = null;
        if (period == null) {
            return PatientIdentifier.Currency.UNKNOWN;
        }
        start = (Date) Versions.invokeGetMethod(period, "getStart");
        end = (Date) Versions.invokeGetMethod(period, "getEnd");
        if (start != null || end != null) {
            Date now = new Date();
            if ((start == null || now.getTime() > start.getTime()) && (end == null || end.getTime() > now.getTime())) {
                return PatientIdentifier.Currency.CURRENT;
            } else {
                return PatientIdentifier.Currency.HISTORIC;
            }
        }
        return PatientIdentifier.Currency.UNKNOWN;
    }

    /**
     * Get the currency information for the different MBI identifier information
     *
     * @param identifier - the FHIR identifier
     * @return the currency information
     */
    private static PatientIdentifier.Currency getCurrency(ICompositeType identifier) {
        String system = getSystem(identifier);
        String value = getValue(identifier);
        if (StringUtils.isAnyBlank(system, value)) {
            return null;
        }
        if (system.equalsIgnoreCase(PatientIdentifier.Type.MBI.getSystem())) {
            return getCurrencyMbi(identifier);
        }
        if (system.equalsIgnoreCase(PatientIdentifier.Type.MBI_R4.getSystem())) {
            return getCurrencyMbiStandard(identifier);
        }
        return PatientIdentifier.Currency.UNKNOWN;
    }

    /**
     * Retreive the extension containing the currency information for STU3 MBI objects
     *
     * @param identifier - the identifier
     * @return the extension
     */
    private static Optional<IBaseExtension> getCurrencyExtension(ICompositeType identifier) {
        List<IBaseExtension> extensions = (List<IBaseExtension>) Versions.invokeGetMethod(identifier, "getExtension");
        if (extensions.isEmpty()) {
            return Optional.empty();
        }

        return extensions.stream().filter(extension -> Versions.invokeGetMethod(extension, "getUrl").equals(CURRENCY_IDENTIFIER))
                .findFirst();
    }
}
