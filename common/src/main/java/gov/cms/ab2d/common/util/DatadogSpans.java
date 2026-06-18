package gov.cms.ab2d.common.util;

import datadog.trace.api.interceptor.MutableSpan;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper for decorating the currently active Datadog span with tags, metrics and error state from
 * application code.
 *
 * <p>The public {@code dd-trace-api} artifact intentionally does not expose an accessor for the
 * active span (its {@code datadog.trace.api.Tracer} has no {@code activeSpan()} method). The
 * dd-java-agent does expose one via {@code datadog.trace.bootstrap.instrumentation.api.AgentTracer},
 * and the {@code AgentSpan} it returns implements {@link MutableSpan} (which lives in
 * {@code dd-trace-api}). We reach that accessor reflectively so this module compiles against the
 * trace API alone while still tagging real spans at runtime when the agent is attached.</p>
 *
 * <p>When the agent is not attached (local runs, unit tests) every method is a safe no-op.</p>
 */
@Slf4j
public final class DatadogSpans {

    private static final String AGENT_TRACER_CLASS =
            "datadog.trace.bootstrap.instrumentation.api.AgentTracer";

    private static final Method ACTIVE_SPAN_METHOD = resolveActiveSpanMethod();

    private DatadogSpans() {
    }

    private static Method resolveActiveSpanMethod() {
        try {
            return Class.forName(AGENT_TRACER_CLASS).getMethod("activeSpan");
        } catch (ReflectiveOperationException | LinkageError e) {
            // Agent not attached - tagging is disabled and all calls become no-ops.
            return null;
        }
    }

    /**
     * @return the currently active span as a {@link MutableSpan}, or {@code null} when no agent is
     *     attached or no span is active.
     */
    public static MutableSpan activeSpan() {
        if (ACTIVE_SPAN_METHOD == null) {
            return null;
        }
        try {
            Object span = ACTIVE_SPAN_METHOD.invoke(null);
            return (span instanceof MutableSpan) ? (MutableSpan) span : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    /** Set a string tag on the active span if one is present. */
    public static void setTag(String key, String value) {
        MutableSpan span = activeSpan();
        if (span != null) {
            span.setTag(key, value);
        }
    }

    /** Set a numeric tag on the active span if one is present. */
    public static void setTag(String key, Number value) {
        MutableSpan span = activeSpan();
        if (span != null) {
            span.setTag(key, value);
        }
    }

    /** Set a boolean tag on the active span if one is present. */
    public static void setTag(String key, boolean value) {
        MutableSpan span = activeSpan();
        if (span != null) {
            span.setTag(key, value);
        }
    }

    /** Set a numeric metric on the active span if one is present. */
    public static void setMetric(String key, long value) {
        MutableSpan span = activeSpan();
        if (span != null) {
            span.setMetric(key, value);
        }
    }

    /** Set a numeric metric on the active span if one is present. */
    public static void setMetric(String key, double value) {
        MutableSpan span = activeSpan();
        if (span != null) {
            span.setMetric(key, value);
        }
    }

    /**
     * Flag the active span as errored and record the error message, mirroring the explicit
     * error-tagging pattern from the Datadog migration guide.
     */
    public static void markError(Throwable throwable) {
        MutableSpan span = activeSpan();
        if (span != null) {
            span.setError(true);
            span.setTag("error.msg", throwable == null ? null : throwable.getMessage());
        }
    }
}
