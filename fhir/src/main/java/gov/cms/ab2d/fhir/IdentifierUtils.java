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

import static gov.cms.ab2d.fhir.ExtensionUtils.*;

@Slf4j
public class IdentifierUtils {
    public static final String CURRENCY_IDENTIFIER =
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency";
    public static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";

    public static String getCurrentMbi(IDomainResource patient) {
        try {
            List identifiers = (List) Versions.invokeGetMethod(patient, "getIdentifier");
            return (String) identifiers.stream()
                    .filter(c -> isMatchingMbi((ICompositeType) c, CURRENT_MBI))
                    .map(c -> {
                        try {
                            return Versions.invokeGetMethod(c, "getValue");
                        } catch (Exception e) {
                            log.error("Unable to get the current MBI value", e);
                            return null;
                        }
                    })
                    .findFirst().orElse(null);
        } catch (Exception e) {
            log.error("Unable to find identifiers in the patient", e);
            return null;
        }
    }

    public static Set<String> getHistoricMbi(IDomainResource patient) {
        try {
            List identifiers = (List) Versions.invokeGetMethod(patient, "getIdentifier");
            return (Set<String>) identifiers.stream()
                    .filter(c -> isMatchingMbi((ICompositeType) c, HISTORIC_MBI))
                    .map(c -> {
                        try {
                            return Versions.invokeGetMethod((IBase) c, "getValue");
                        } catch (Exception e) {
                            log.error("Unable to get the historic MBI value", e);
                            return null;
                        }
                    })
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Unable to find historic MBIs in the patient", e);
            return null;
        }
    }

    public static String getBeneId(IDomainResource patient) {
        try {
            List identifiers = (List) Versions.invokeGetMethod(patient, "getIdentifier");
            return (String) identifiers.stream()
                    .filter(c -> isBeneficiaryId((ICompositeType) c))
                    .map(c -> {
                        try {
                            return Versions.invokeGetMethod((IBase) c, "getValue");
                        } catch (Exception e) {
                            log.error("Unable to get the current MBI value", e);
                            return null;
                        }
                    })
                    .findFirst().orElse(null);
        } catch (Exception e) {
            log.error("Unable to find identifiers in the patient", e);
            return null;
        }
    }

    private static String getSystem(ICompositeType identifier) {
        try {
            return (String) Versions.invokeGetMethod(identifier, "getSystem");
        } catch (Exception e) {
            log.error("Unable to extract system from identifier", e);
            return null;
        }
    }

    private static String getValue(ICompositeType identifier) {
        try {
            return (String) Versions.invokeGetMethod(identifier, "getValue");
        } catch (Exception e) {
            log.error("Unable to extract value from identifier", e);
            return null;
        }
    }

    private static boolean isBeneficiaryId(ICompositeType identifier) {
        try {
            String system = getSystem(identifier);
            String value = getValue(identifier);
            if (StringUtils.isAnyBlank(system, value)) {
                return false;
            }
            return system.equalsIgnoreCase(BENEFICIARY_ID);
       } catch (Exception e) {
            log.error("Unable to extract Beneficiary Id", e);
            return false;
       }
    }

    private static boolean isMatchingMbi(ICompositeType identifier, String historic) {
        try {
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
        } catch (Exception e) {
            log.error("Unable to extract MBI values", e);
            return false;
        }
    }

    private static Optional<IBaseExtension> getCurrencyExtension(ICompositeType identifier) {
        List<IBaseExtension> extensions = null;
        try {
            extensions = (List<IBaseExtension>) Versions.invokeGetMethod(identifier, "getExtension");
        } catch (Exception e) {
            return Optional.of(null);
        }

        if (extensions.isEmpty()) {
            return Optional.empty();
        }

        return extensions.stream().filter(
                extension -> {
                    try {
                        return (Versions.invokeGetMethod(extension, "getUrl")).equals(CURRENCY_IDENTIFIER);
                    } catch (Exception e) {
                        log.error("Unable to get URL from extension");
                        return false;
                    }
                }
        ).findFirst();
    }
}
