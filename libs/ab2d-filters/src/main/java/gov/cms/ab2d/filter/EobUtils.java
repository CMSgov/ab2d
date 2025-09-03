package gov.cms.ab2d.filter;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.filter.EOBLoadUtilities.EOB_TYPE_CODE_SYS;
import static gov.cms.ab2d.filter.EOBLoadUtilities.EOB_TYPE_PART_D_CODE_VAL;

@Slf4j
@UtilityClass
public class EobUtils {
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
     * Return the EOB.getBillablePeriod date range for different FHIR versions
     *
     * @param ben - the EOB
     * @return the billable period
     */
    public static Object getBillablePeriod(IBaseResource ben) {
        if (ben == null) {
            return null;
        }
        return EobUtils.invokeGetMethod(ben, "getBillablePeriod");
    }

    /**
     * Get start date of a billable period
     *
     * @param ben the EOB object
     * @return the start date
     */
    public static Date getStartDate(IBaseResource ben) {
        Object period = getBillablePeriod(ben);
        if (period == null) {
            return null;
        }
        return (Date) EobUtils.invokeGetMethod(period, "getStart");
    }

    /**
     * Get end date of a billable period
     *
     * @param ben the EOB object
     * @return the end date
     */
    public static Date getEndDate(IBaseResource ben) {
        Object period = getBillablePeriod(ben);
        if (period == null) {
            return null;
        }
        return (Date) EobUtils.invokeGetMethod(period, "getEnd");
    }

    /**
     * Returns true if an EOB is part D
     *
     * @param eob - the EOB
     * @return 0 if the EOB is part D
     */
    public static boolean isPartD(IBaseResource eob) {
        if (eob == null) {
            return false;
        }
        if (!eob.fhirType().endsWith("ExplanationOfBenefit")) {
            return false;
        }
        Object c = EobUtils.invokeGetMethod(eob, "getType");
        List<?> codes = (List<?>) EobUtils.invokeGetMethod(c, "getCoding");
        return codes.stream()
                .filter(code -> ((String) EobUtils.invokeGetMethod(code, "getSystem")).endsWith(EOB_TYPE_CODE_SYS))
                .anyMatch(code -> EOB_TYPE_PART_D_CODE_VAL.equalsIgnoreCase((String) EobUtils.invokeGetMethod(code, "getCode")));
    }
}
