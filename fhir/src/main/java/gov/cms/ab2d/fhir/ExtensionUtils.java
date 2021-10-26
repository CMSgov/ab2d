package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.*;

import java.util.List;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.stream.Collectors;

import static gov.cms.ab2d.fhir.PatientIdentifier.CURRENT_MBI;
import static gov.cms.ab2d.fhir.PatientIdentifier.HISTORIC_MBI;
import static gov.cms.ab2d.fhir.PatientIdentifier.MBI_ID;
import static gov.cms.ab2d.fhir.IdentifierUtils.CURRENCY_IDENTIFIER;

/**
 * Used to support manipulation of extensions to Resource objects for different FHIR versions
 */
@Slf4j
public final class ExtensionUtils {

    public static final String ID_EXT = "http://hl7.org/fhir/StructureDefinition/elementdefinition-identifier";
    public static final String REF_YEAR_EXT = "https://bluebutton.cms.gov/resources/variables/rfrnc_yr";

    private ExtensionUtils() { }

    /**
     * Add an extension to a resource
     *
     * @param resource - the resource
     * @param extension - the extension
     * @param version - the FHIR version
     */
    public static void addExtension(IBaseResource resource, IBase extension, FhirVersion version) {
        if (resource == null || extension == null) {
            return;
        }
        try {
            Versions.invokeSetMethod(resource, "addExtension", extension, Class.forName(version.getClassName("Extension")));
        } catch (Exception ex) {
            log.error("Unable to add Extension");
        }
    }

    /**
     * Create an MBI extension
     *
     * @param mbi - the value of the MBI
     * @param current - if it is a current or historical MBI
     * @param version - the FHIR version
     * @return the extension
     */
    public static IBase createMbiExtension(String mbi, boolean current, FhirVersion version) {
        Object identifier = Versions.getObject(version, "Identifier");
        Versions.invokeSetMethod(identifier, "setSystem", MBI_ID, String.class);
        Versions.invokeSetMethod(identifier, "setValue", mbi, String.class);

        Object coding = Versions.getObject(version, "Coding");
        Versions.invokeSetMethod(coding, "setCode", current ? CURRENT_MBI : HISTORIC_MBI, String.class);

        Object currencyExtension = Versions.getObject(version, "Extension");
        Versions.invokeSetMethod(currencyExtension, "setUrl", CURRENCY_IDENTIFIER, String.class);
        try {
            Versions.invokeSetMethod(currencyExtension, "setValue", coding, Class.forName(version.getClassName("Type")));
        } catch (Exception ex) {
            log.error("Unable to setValue");
        }

        Versions.invokeSetMethod(identifier, "setExtension", List.of(currencyExtension), List.class);

        Object ext = Versions.getObject(version, "Extension");
        Versions.invokeSetMethod(ext, "setUrl", ID_EXT, String.class);
        try {
            Versions.invokeSetMethod(ext, "setValue", identifier, Class.forName(version.getClassName("Type")));
        } catch (Exception ex) {
            log.error("Unable to setValue");
        }
        return (IBase) ext;
    }

    /**
     * Get the reference year for the patient from the extension
     *
     * @param patient - the patient resource
     * @return the year
     */
    @SuppressWarnings("unchecked")
    public static int getReferenceYear(IDomainResource patient) {
        List<? extends IBaseExtension> refYearList = patient.getExtension().stream()
                .filter(c -> REF_YEAR_EXT.equalsIgnoreCase(c.getUrl()))
                .collect(Collectors.toList());
        if (refYearList.isEmpty()) {
            return -1;
        }
        IBaseExtension ext = refYearList.get(0);
        IPrimitiveType<Date> date = (IPrimitiveType<Date>) ext.getValue();
        Date dateVal = date.getValue();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(dateVal);
        return cal.get(Calendar.YEAR);
    }
}
