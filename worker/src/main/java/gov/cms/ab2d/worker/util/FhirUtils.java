package gov.cms.ab2d.worker.util;

import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.fhir.ExtensionUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

@Slf4j
public class FhirUtils {

    public static void addMbiIdsToEobs(IBaseResource eob, CoverageSummary patient, FhirVersion version) {
        if (eob == null) {
            return;
        }

        // Add extesions only if beneficiary id is present and known to memberships
        Long benId = EobUtils.getPatientId(eob);
        if (benId != null && patient != null) {
            Identifiers identifiers = patient.getIdentifiers();

            // Add each mbi to each eob
            if (identifiers.getCurrentMbi() != null) {
                IBase currentMbiExtension = ExtensionUtils.createMbiExtension(identifiers.getCurrentMbi(), true, version);
                ExtensionUtils.addExtension(eob, currentMbiExtension, version);
            }

            for (String mbi : identifiers.getHistoricMbis()) {
                IBase mbiExtension = ExtensionUtils.createMbiExtension(mbi, false, version);
                ExtensionUtils.addExtension(eob, mbiExtension, version);
            }
        }
    }
}
