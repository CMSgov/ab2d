package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.cms.ab2d.fhir.ExtensionUtils.CURRENT_MBI;
import static gov.cms.ab2d.fhir.ExtensionUtils.HISTORIC_MBI;
import static gov.cms.ab2d.fhir.ExtensionUtils.MBI_ID;

/**
 * Util methods to manipulate identifiers for different FHIR versions
 */
@Slf4j
public final class IdentifierUtils {
    public static final String CURRENCY_IDENTIFIER =
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency";
    public static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";

    private IdentifierUtils() { }

    /**
     * Return the current MBI from the Patient resource
     *
     * @param patient - the patient
     * @return the current MBI
     */
    public static String getCurrentMbi(IDomainResource patient) {
        if (patient == null) {
            return null;
        }
        List identifiers = (List) Versions.invokeGetMethod(patient, "getIdentifier");
        return (String) identifiers.stream()
                .filter(c -> isMatchingMbi((ICompositeType) c, CURRENT_MBI))
                .map(c -> Versions.invokeGetMethod(c, "getValue"))
                .findFirst().orElse(null);
    }

    /**
     * Return the historical MBIs from the Patient resource
     *
     * @param patient - the patient resource
     * @return the set of historical MBIs
     */
    public static Set<String> getHistoricMbi(IDomainResource patient) {
        if (patient == null) {
            return null;
        }
        List identifiers = (List) Versions.invokeGetMethod(patient, "getIdentifier");
        return (Set<String>) identifiers.stream()
                .filter(c -> isMatchingMbi((ICompositeType) c, HISTORIC_MBI))
                .map(c -> Versions.invokeGetMethod((IBase) c, "getValue"))
                .collect(Collectors.toSet());
    }

    /**
     * Return the beneficiary id for a patient
     *
     * @param patient - the patient resource
     * @return the ben_id
     */
    public static String getBeneId(IDomainResource patient) {
        if (patient == null) {
            return null;
        }
        List identifiers = (List) Versions.invokeGetMethod(patient, "getIdentifier");
        return (String) identifiers.stream()
                .filter(c -> isBeneficiaryId((ICompositeType) c))
                .map(c -> Versions.invokeGetMethod((IBase) c, "getValue"))
                .findFirst().orElse(null);
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

    private static boolean isBeneficiaryId(ICompositeType identifier) {
        String system = getSystem(identifier);
        String value = getValue(identifier);
        if (StringUtils.isAnyBlank(system, value)) {
            return false;
        }
        return system.equalsIgnoreCase(BENEFICIARY_ID);
    }

    private static boolean isMatchingMbi(ICompositeType identifier, String historic) {
        String system = getSystem(identifier);
        String value = getValue(identifier);
        if (StringUtils.isAnyBlank(system, value)) {
            return false;
        }
        if (!system.equals(MBI_ID)) {
            return false;
        }
        Optional<IBaseExtension> currencyExtension = getCurrencyExtension(identifier);
        if (currencyExtension.isEmpty()) {
            return false;
        }
        Object code = Versions.invokeGetMethod(currencyExtension.get(), "getValue");
        return Versions.invokeGetMethod(code, "getCode").equals(historic);
    }

    private static Optional<IBaseExtension> getCurrencyExtension(ICompositeType identifier) {
        List<IBaseExtension> extensions = (List<IBaseExtension>) Versions.invokeGetMethod(identifier, "getExtension");
        if (extensions.isEmpty()) {
            return Optional.empty();
        }

        return extensions.stream().filter(extension -> Versions.invokeGetMethod(extension, "getUrl").equals(CURRENCY_IDENTIFIER))
                .findFirst();
    }
}
