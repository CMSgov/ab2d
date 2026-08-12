package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.parser.IParser;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes a beneficiary's EOBs to ndjson with error isolation. Resources that fail to encode
 * get added as an OperationOutcome line in the error file instead of failing the whole bene
 */
@Slf4j
public final class EobNdjsonSerializer {

    private EobNdjsonSerializer() {
    }

    /**
     * Encode each resource to a string and add it to the appropriate list, to be later flushed
     * into a file.
     */
    public static SerializedEobs serialize(FhirVersion version, List<IBaseResource> eobs) {
        if (eobs == null || eobs.isEmpty()) {
            return new SerializedEobs(List.of(), List.of());
        }
        IParser parser = version.getJsonParser().setPrettyPrint(false);
        List<String> dataLines = new ArrayList<>(eobs.size());
        List<String> errorLines = new ArrayList<>();
        for (IBaseResource resource : eobs) {
            try {
                dataLines.add(parser.encodeResourceToString(resource));
            } catch (Exception ex) {
                log.warn("Encountered exception while serializing job resources: {}", ex.getClass());
                String errMsg = ExceptionUtils.getRootCauseMessage(ex);
                IBaseResource operationOutcome = version.getErrorOutcome(errMsg);
                errorLines.add(parser.encodeResourceToString(operationOutcome));
            }
        }
        return new SerializedEobs(dataLines, errorLines);
    }
}
