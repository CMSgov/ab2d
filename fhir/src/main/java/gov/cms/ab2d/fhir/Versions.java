package gov.cms.ab2d.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Versions {
    public enum FhirVersions {
        R3,
        R4
    }

    private static Map<FhirVersions, String> classLocations = new HashMap<>() {
        { put (FhirVersions.R3, "org.hl7.fhir.dstu3.model"); }
        { put (FhirVersions.R4, "org.hl7.fhir.r4.model"); }
    };

    private static Map<FhirVersions, FhirContext> fhirContexts = new HashMap<>() {
        { put (FhirVersions.R3, FhirContext.forDstu3()); }
        { put (FhirVersions.R4, FhirContext.forR4()); }
    };

    private static List<String> supportedClasses = List.of(
            "ExplanationOfBenefit",
            "Patient",
            "Identifier",
            "Bundle",
            "Extension",
            "Coding",
            "Type",
            "OperationOutcome",
            "CodeableConcept",
            "OperationOutcome.OperationOutcomeIssueComponent",
            "OperationOutcome.IssueSeverity",
            "OperationOutcome.IssueType",
            "Enumerations.PublicationStatus",
            "DateTimeType",
            "ResourceType",
            "Period"
    );

    private static Map<FhirVersionEnum, FhirVersions> supportedFhirVersion = new HashMap<>() {
        { put (FhirVersionEnum.DSTU3, FhirVersions.R3); }
        { put (FhirVersionEnum.R4, FhirVersions.R4); }
    };

    private static Map<String, FhirVersions> apiVersionToFhirVersion = new HashMap<>() {
        { put ("/v1/", FhirVersions.R3); }
        { put ("/v2/", FhirVersions.R4); }
    };

    public static FhirVersions getVersionFromUrl(String url) {
        FhirVersions version = FhirVersions.R3;
        String versionKey = apiVersionToFhirVersion.entrySet().stream()
                .map(c -> c.getKey())
                .filter(c -> url.contains(c))
                .findFirst().orElse(null);
        if (versionKey == null) {
            return version;
        }
        return apiVersionToFhirVersion.get(versionKey);
    }

    public static String getClassName(FhirVersions version, String name) {
        String base = classLocations.get(version);
        if (base == null) {
            throw new VersionNotSupported(version.toString() + " is not supported for " + name);
        }
        return base + "." + name;
    }

    public static Object instantiateClass(FhirVersions version, String objName) {
        try {
            if (!supportedClasses.contains(objName)) {
                throw new RuntimeException("Class " + objName + " is not supported");
            }
            String name = getClassName(version, objName);
            Class clazz = Class.forName(name);
            Object obj = clazz.getDeclaredConstructor(null).newInstance();
            return obj;
        } catch (Exception ex) {
            log.error("Unable to instantiate " + objName + " class", ex);
            return null;
        }
    }

    static Object instantiateClassWithParam(FhirVersions version, String objName, Object arg, Class argClass) {
        if (!supportedClasses.contains(objName)) {
            throw new RuntimeException("Class " + objName + " is not supported");
        }
        String name = getClassName(version, objName);
        try {
            Class clazz = Class.forName(name);
            Object obj = clazz.getDeclaredConstructor(argClass).newInstance(arg);
            return obj;
        } catch (Exception ex) {
            log.error("Unable to instantiate " + objName + " with  " + argClass.getName() + " class", ex);
            return null;

        }
    }

    static Object invokeGetMethod(Object resource, String methodName) {
        try {
            Method method = resource.getClass().getMethod(methodName);
            return method.invoke(resource);
        } catch (Exception ex) {
            log.error("Unable to invoke get method " + methodName + " on " + resource.getClass().getName());
            return null;
        }
    }

    static Object invokeGetMethodWithArg(Object resource, String methodName, Object arg, Class clazz) {
        try {
            Method method = resource.getClass().getMethod(methodName, clazz);
            return method.invoke(resource, arg);
        } catch (Exception ex) {
            log.error("Unable to invoke get method " + methodName + " on " + resource.getClass().getName() + " with " + clazz.getName() + " argument", ex);
            return null;
        }
    }

    static void invokeSetMethod(Object resource, String methodName, Object val, Class paramType) {
        try {
            Method method = resource.getClass().getMethod(methodName, paramType);
            method.invoke(resource, val);
        } catch (Exception ex) {
            log.error("Unable to invoke set method " + methodName + " on " + resource.getClass().getName() + " with " + val + " argument", ex);
        }
    }

    public static Object instantiateEnum(FhirVersions version, String cName, String value) {
        try {
            String topClassName = getClassName(version, cName);
            Class clazz = Class.forName(topClassName);
            Method valueOf = clazz.getMethod("valueOf", String.class);
            return valueOf.invoke(null, value);
        } catch (Exception ex) {
            log.error("Unable to instantiate enum " + cName + " with value " + value, ex);
            return null;
        }
    }

    public static Object instantiateEnum(FhirVersions version, String topLevel, String lowerLevel, String value) {
        try {
            String topClassName = getClassName(version, topLevel);
            Class top = Class.forName(topClassName);
            Class[] classes = top.getClasses();
            for (Class c : classes) {
                if (c.getName().endsWith("$" + lowerLevel)) {
                    Method valueOf = c.getMethod("valueOf", String.class);
                    return valueOf.invoke(null, value);
                }
            }
            return null;
        } catch (Exception ex) {
            log.error("Unable to instantiate enum " + topLevel + "." + lowerLevel + " with value " + value, ex);
            return null;
        }
    }

    public static Object instantiateClass(FhirVersions version, String topLevel, String lowerLevel) {
        try {
            String name = getClassName(version, topLevel);
            Class clazz = Class.forName(name);
            String className = topLevel + "." + lowerLevel;
            if (!supportedClasses.contains(className)) {
                throw new RuntimeException("Class " + className + " is not supported");
            }
            Class[] classes = clazz.getClasses();
            for (Class c : classes) {
                if (c.getName().endsWith("$" + lowerLevel)) {
                    return c.getDeclaredConstructor(null).newInstance();
                }
            }
            return null;
        } catch (Exception ex) {
            log.error("Unable to instantiate " + topLevel + "." + lowerLevel, ex);
            return null;
        }
    }

    public static FhirVersions getVersion(FhirContext context) {
        if (context == null || context.getVersion() == null) {
            throw new VersionNotSupported("Null context passed");
        }
        FhirVersionEnum v = context.getVersion().getVersion();
        FhirVersions version = supportedFhirVersion.get(v);
        if (version == null) {
            throw new VersionNotSupported(v.getFhirVersionString() + " is not supported");
        }
        return version;
    }

    public static FhirContext getContextFromVersion(FhirVersions version) {
        if (version == null) {
            return null;
        }
        return fhirContexts.get(version);
    }
}
