package gov.cms.ab2d.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.*;

/**
 * This class provides a lot of underlying methods for the other Util classes including:
 *   1. Defining supported versions
 *   2. Defining valid classes
 *   3. Creating enumerations of versions and mapping between FHIR context objects and those versions
 *   4. Package private methods to use reflection to instantiate classes, enums and to invoke set/get for those classes
 *   5. The ability to determine which version is being used based on the URL passed from the HttpRequest
 */
@Slf4j
public class Versions {
    public enum FhirVersions {
        R3,
        R4
    }

    private static final Map<FhirVersions, String> CLASS_LOCATIONS = new EnumMap<>(FhirVersions.class) {
        { put (FhirVersions.R3, "org.hl7.fhir.dstu3.model"); }
        { put (FhirVersions.R4, "org.hl7.fhir.r4.model"); }
    };

    private static final Map<FhirVersions, FhirContext> FHIR_CONTEXTS = new EnumMap<>(FhirVersions.class) {
        { put (FhirVersions.R3, FhirContext.forDstu3()); }
        { put (FhirVersions.R4, FhirContext.forR4()); }
    };

    /**
     * Currently, the classes in the model directories that we can instantiate
     */
    private static final Set<String> SUPPORTED_CLASSES = Set.of(
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

    private static final Map<FhirVersionEnum, FhirVersions> SUPPORTED_FHIR_VERSION = new EnumMap<>(FhirVersionEnum.class) {
        { put (FhirVersionEnum.DSTU3, FhirVersions.R3); }
        { put (FhirVersionEnum.R4, FhirVersions.R4); }
    };

    private static final Map<String, FhirVersions> API_VERSION_TO_FHIR_VERSION = new HashMap<>() {
        { put ("/v1/", FhirVersions.R3); }
        { put ("/v2/", FhirVersions.R4); }
    };

    /**
     * Given a URL passed to the application, return the FHIR version used
     *
     * @param url - the URL
     * @return the FHIR version
     */
    public static FhirVersions getVersionFromUrl(String url) {
        FhirVersions version = FhirVersions.R3;
        String versionKey = API_VERSION_TO_FHIR_VERSION.keySet().stream()
                .filter(url::contains)
                .findFirst().orElse(null);
        if (versionKey == null) {
            return version;
        }
        return API_VERSION_TO_FHIR_VERSION.get(versionKey);
    }

    /**
     * Given a FHIR version and the name of a class, return the proper class for the version
     *
     * @param version - the FHIR version
     * @param name - the name of the class
     * @return the class object
     */
    public static String getClassName(FhirVersions version, String name) {
        String base = CLASS_LOCATIONS.get(version);
        if (base == null) {
            throw new VersionNotSupported(version.toString() + " is not supported for " + name);
        }
        return base + "." + name;
    }

    /**
     * Given a FHIR version and object name, instantiate the object for the correct version
     *
     * @param version - the FHIR version
     * @param objName - the object name
     * @return the instantiated class (the default constructor with no parameters)
     */
    public static Object instantiateClass(FhirVersions version, String objName) {
        try {
            if (!SUPPORTED_CLASSES.contains(objName)) {
                throw new RuntimeException("Class " + objName + " is not supported");
            }
            String name = getClassName(version, objName);
            Class<?> clazz = Class.forName(name);
            return clazz.getDeclaredConstructor(null).newInstance();
        } catch (Exception ex) {
            log.error("Unable to instantiate " + objName + " class", ex);
            return null;
        }
    }

    /**
     * Given a FHIR version and object name, instantiate the object for the correct version with one argument
     *
     * @param version - the FHIR version
     * @param objName - the object name
     * @param arg - the argument
     * @param argClass - the class type of the argument
     * @return the instantiated object
     */
    static Object instantiateClassWithParam(FhirVersions version, String objName, Object arg, Class argClass) {
        if (!SUPPORTED_CLASSES.contains(objName)) {
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

    /**
     * Given a resource and the method name, return the result of calling the method
     *
     * @param resource - the resource object
     * @param methodName - the method name
     * @return the result of calling the method
     */
    static Object invokeGetMethod(Object resource, String methodName) {
        try {
            Method method = resource.getClass().getMethod(methodName);
            return method.invoke(resource);
        } catch (Exception ex) {
            log.error("Unable to invoke get method " + methodName + " on " + resource.getClass().getName());
            return null;
        }
    }

    /**
     * Given a resource, the method name and an argument, return the result of calling the method with a parameter
     *
     * @param resource - the resource
     * @param methodName - the method name
     * @param arg - the argument
     * @param clazz - the argument class
     * @return the result of calling the method
     */
    static Object invokeGetMethodWithArg(Object resource, String methodName, Object arg, Class clazz) {
        try {
            Method method = resource.getClass().getMethod(methodName, clazz);
            return method.invoke(resource, arg);
        } catch (Exception ex) {
            log.error("Unable to invoke get method " + methodName + " on " + resource.getClass().getName() + " with " + clazz.getName() + " argument", ex);
            return null;
        }
    }

    /**
     * Given a resource, method name and an argument, set a value (or call a method with a void return type)
     *
     * @param resource - the resource
     * @param methodName - the method name
     * @param val - the value of the parameter
     * @param paramType - the class of the parameter
     */
    static void invokeSetMethod(Object resource, String methodName, Object val, Class paramType) {
        try {
            Method method = resource.getClass().getMethod(methodName, paramType);
            method.invoke(resource, val);
        } catch (Exception ex) {
            log.error("Unable to invoke set method " + methodName + " on " + resource.getClass().getName() + " with " + val + " argument", ex);
        }
    }

    /**
     * Instantiate enum from the version, enum name and value of the enum
     *
     * @param version - the FHIR version
     * @param cName - the enum name
     * @param value - the enum value
     * @return the enum
     */
    static Object instantiateEnum(FhirVersions version, String cName, String value) {
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

    /**
     * Instantiate enum  the version, enum name (class and internal enum name) and value of the enum
     *
     * @param version - the FHIR version
     * @param topLevel - the top level class
     * @param lowerLevel - the internal enum
     * @param value - the enum value
     * @return the enum
     */
    static Object instantiateEnum(FhirVersions version, String topLevel, String lowerLevel, String value) {
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

    /**
     * Instantiate an internal class from a version and class names
     *
     * @param version - the FHIR version
     * @param topLevel - the class name
     * @param lowerLevel - the internal class name
     * @return the object of that type
     */
    static Object instantiateClass(FhirVersions version, String topLevel, String lowerLevel) {
        try {
            String name = getClassName(version, topLevel);
            Class<?> clazz = Class.forName(name);
            String className = topLevel + "." + lowerLevel;
            if (!SUPPORTED_CLASSES.contains(className)) {
                throw new RuntimeException("Class " + className + " is not supported");
            }
            Class<?>[] classes = clazz.getClasses();
            for (Class<?> c : classes) {
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

    /**
     * Convert from a context to FHIR version
     *
     * @param context - the FhirContext
     * @return the FHIR version
     */
    public static FhirVersions getVersion(FhirContext context) {
        if (context == null || context.getVersion() == null) {
            throw new VersionNotSupported("Null context passed");
        }
        FhirVersionEnum v = context.getVersion().getVersion();
        FhirVersions version = SUPPORTED_FHIR_VERSION.get(v);
        if (version == null) {
            throw new VersionNotSupported(v.getFhirVersionString() + " is not supported");
        }
        return version;
    }

    /**
     * Given a version, return the FhirContext
     *
     * @param version - the version
     * @return the FHIRContext
     */
    public static FhirContext getContextFromVersion(FhirVersions version) {
        if (version == null) {
            return null;
        }
        return FHIR_CONTEXTS.get(version);
    }
}
