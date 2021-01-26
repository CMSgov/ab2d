package gov.cms.ab2d.common.util.fhir;

import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.fhir.ExtensionUtils;
import gov.cms.ab2d.fhir.Versions;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;
import java.util.Map;

@Slf4j
public class FhirUtils {
    public static void addMbiIdsToEobs(List<IBaseResource> eobs, Map<String, CoverageSummary> patients, Versions.FHIR_VERSIONS version) {
        if (eobs == null || eobs.isEmpty()) {
            return;
        }
        // Get first EOB Bene ID
        IBaseResource eob = eobs.get(0);

        // Add extesions only if beneficiary id is present and known to memberships
        String benId = EobUtils.getPatientId(eob);
        if (benId != null && patients.containsKey(benId)) {
            Identifiers patient = patients.get(benId).getIdentifiers();

            // Add each mbi to each eob
            if (patient.getCurrentMbi() != null) {
                IBase currentMbiExtension = ExtensionUtils.createExtension(eob, patient.getCurrentMbi(), true, version);
                eobs.forEach(e -> ExtensionUtils.addExtension(e, currentMbiExtension, version));
            }

            for (String mbi : patient.getHistoricMbis()) {
                IBase mbiExtension = ExtensionUtils.createExtension(eob, mbi, false, version);
                eobs.forEach(e -> ExtensionUtils.addExtension(e, mbiExtension, version));
            }
        }
    }
}
