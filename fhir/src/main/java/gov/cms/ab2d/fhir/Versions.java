package gov.cms.ab2d.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Versions {
    public enum FHIR_VERSIONS {
        R3,
        R4
    }

    private static Map<FHIR_VERSIONS, String> classLocations = new HashMap<>() {
        { put (FHIR_VERSIONS.R3, "org.hl7.fhir.dstu3.model"); }
        { put (FHIR_VERSIONS.R4, "org.hl7.fhir.r4.model"); }
    };

    private static Map<FHIR_VERSIONS, FhirContext> fhirContexts = new HashMap<>() {
        { put (FHIR_VERSIONS.R3, FhirContext.forDstu3()); }
        { put (FHIR_VERSIONS.R4, FhirContext.forR4()); }
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
            "ResourceType"
    );

    private static Map<FhirVersionEnum, FHIR_VERSIONS> supportedFhirVersion = new HashMap<>() {
        { put (FhirVersionEnum.DSTU3, FHIR_VERSIONS.R3); }
        { put (FhirVersionEnum.R4, FHIR_VERSIONS.R4); }
    };

    private static Map<String, FHIR_VERSIONS> apiVersionToFhirVersion = new HashMap<>() {
        { put ("/v1/", FHIR_VERSIONS.R3); }
        { put ("/v2/", FHIR_VERSIONS.R4); }
    };

    public static FHIR_VERSIONS getVersionFromUrl(String url) {
        FHIR_VERSIONS version = FHIR_VERSIONS.R3;
        String versionKey = apiVersionToFhirVersion.entrySet().stream()
                .map(c -> c.getKey())
                .filter(c -> url.contains(c))
                .findFirst().orElse(null);
        if (versionKey == null) {
            return version;
        }
        return apiVersionToFhirVersion.get(versionKey);
    }

    public static String getClassName(FHIR_VERSIONS version, String name) throws VersionNotSupported {
        String base = classLocations.get(version);
        if (base == null) {
            throw new VersionNotSupported(version.toString() + " is not supported for " + name);
        }
        return base + "." + name;
    }

    public static Object instantiateClass(FHIR_VERSIONS version, String objName) throws VersionNotSupported, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (!supportedClasses.contains(objName)) {
            throw new RuntimeException("Class " + objName + " is not supported");
        }
        String name = getClassName(version, objName);
        Class clazz = Class.forName(name);
        Object obj = clazz.getDeclaredConstructor(null).newInstance();
        return obj;
    }

    public static Object instantiateClassWithParam(FHIR_VERSIONS version, String objName, Object arg, Class argClass) throws VersionNotSupported, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (!supportedClasses.contains(objName)) {
            throw new RuntimeException("Class " + objName + " is not supported");
        }
        String name = getClassName(version, objName);
        Class clazz = Class.forName(name);
        Object obj = clazz.getDeclaredConstructor(argClass).newInstance(arg);
        return obj;
    }

    public static Object invokeGetMethod(Object resource, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = resource.getClass().getMethod(methodName);
        return method.invoke(resource);
    }

    public static Object invokeGetMethodWithArg(Object resource, String methodName, Object arg, Class clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = resource.getClass().getMethod(methodName, clazz);
        Object obj = method.invoke(resource, arg);
        return obj;
    }

    public static void invokeSetMethod(Object resource, String methodName, Object val, Class paramType) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = resource.getClass().getMethod(methodName, paramType);
        method.invoke(resource, val);
    }

    public static Object instantiateEnum(FHIR_VERSIONS version, String cName, String value) throws VersionNotSupported, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        String topClassName = getClassName(version, cName);
        Class clazz = Class.forName(topClassName);
        Method valueOf = clazz.getMethod("valueOf", String.class);
        return valueOf.invoke(null, value);
    }

    public static Object instantiateEnum(FHIR_VERSIONS version, String topLevel, String lowerLevel, String value) throws VersionNotSupported, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
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
    }

    public static Object instantiateClass(FHIR_VERSIONS version, String topLevel, String lowerLevel) throws VersionNotSupported, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
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
    }

    public static FHIR_VERSIONS getVersion(FhirContext context) throws VersionNotSupported {
        if (context == null || context.getVersion() == null) {
            throw new VersionNotSupported("Null context passed");
        }
        FhirVersionEnum v = context.getVersion().getVersion();
        FHIR_VERSIONS version = supportedFhirVersion.get(v);
        if (version == null) {
            throw new VersionNotSupported(v.getFhirVersionString() + " is not supported");
        }
        return version;
    }

    public static FhirContext getContextFromVersion(FHIR_VERSIONS version) {
        if (version == null) {
            return null;
        }
        return fhirContexts.get(version);
    }
}
