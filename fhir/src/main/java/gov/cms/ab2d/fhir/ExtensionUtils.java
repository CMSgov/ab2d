package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ExtensionUtils {
    public static final String CURRENT_MBI = "current";
    public static final String HISTORIC_MBI = "historic";
    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";
    public static final String CURRENCY_IDENTIFIER =
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency";
    static final String ID_EXT = "http://hl7.org/fhir/StructureDefinition/elementdefinition-identifier";
    public static final String REF_YEAR_EXT = "https://bluebutton.cms.gov/resources/variables/rfrnc_yr";

    public static void addExtension(IBaseResource resource, IBase extension, Versions.FHIR_VERSIONS version) {
        try {
            Versions.invokeSetMethod(resource, "addExtension", extension, Class.forName(Versions.getClassName(version, "Extension")));
        } catch (Exception ex) {
            log.error("Unable to add extension to resource", ex);
        }
    }

    public static IBase createExtension(IBaseResource resource, String mbi, boolean current, Versions.FHIR_VERSIONS version) {
        try {
            Object identifier = Versions.instantiateClass(version, "Identifier");
            Versions.invokeSetMethod(identifier, "setSystem", MBI_ID, String.class);
            Versions.invokeSetMethod(identifier, "setValue", mbi, String.class);

            Object coding = Versions.instantiateClass(version, "Coding");
            Versions.invokeSetMethod(coding, "setCode", current ? CURRENT_MBI : HISTORIC_MBI, String.class);

            Object currencyExtension = Versions.instantiateClass(version, "Extension");
            Versions.invokeSetMethod(currencyExtension, "setUrl", CURRENCY_IDENTIFIER, String.class);
            Versions.invokeSetMethod(currencyExtension, "setValue", coding, Class.forName(Versions.getClassName(version, "Type")));

            Versions.invokeSetMethod(identifier, "setExtension", List.of(currencyExtension), java.util.List.class);

            Object ext = Versions.instantiateClass(version, "Extension");
            Versions.invokeSetMethod(ext, "setUrl", ID_EXT, String.class);
            Versions.invokeSetMethod(ext, "setValue", identifier, Class.forName(Versions.getClassName(version, "Type")));
            return (IBase) ext;
        } catch (Exception ex) {
            log.error("Unable to create extension for MBI " + mbi, ex);
            return null;
        }
    }

    public static int getReferenceYear(IDomainResource patient) {
        List<? extends IBaseExtension> refYearList = patient.getExtension().stream()
                .filter(c -> REF_YEAR_EXT.equalsIgnoreCase(c.getUrl()))
                .collect(Collectors.toList());
        if (refYearList.size() == 0) {
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
