package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;

public class ConvertXmlToJson {
    public static void main(String ... args) throws IOException {
        String inputFile = args[0];
        String outputFile = args[1];
        InputStream sampleData =
                BlueButtonClientSTU3Test.class.getClassLoader().getResourceAsStream(inputFile);

        if (sampleData == null) {
            throw new MissingResourceException("Cannot find sample requests",
                    BlueButtonClientSTU3Test.class.getName(), inputFile);
        }

        String xmlFileContents = new String(sampleData.readAllBytes(), StandardCharsets.UTF_8);
        IParser xmlParser = getXmlParser(FhirContext.forDstu3());
        IBaseResource resource = xmlParser.parseResource(xmlFileContents);
        IParser jsonParser = getJsonParser(FhirContext.forDstu3());
        String jsonString = jsonParser.encodeResourceToString(resource);
        System.out.println(jsonString);
        FileUtils.writeStringToFile(new File(outputFile), jsonString, "UTF-8");
    }

    private static IParser getXmlParser(FhirContext context) {
        EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.XML_PLAIN_STRING);
        return respType.newParser(context);
    }

    private static IParser getJsonParser(FhirContext context) {
        EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        return respType.newParser(context);
    }
}
