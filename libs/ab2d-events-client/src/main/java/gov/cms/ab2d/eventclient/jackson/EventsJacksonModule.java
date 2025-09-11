package gov.cms.ab2d.eventclient.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import gov.cms.ab2d.eventclient.events.*;

public class EventsJacksonModule extends SimpleModule {

    public EventsJacksonModule() {
        super("EventsJacksonModule");
        registerSubtypes(
                new NamedType(ApiRequestEvent.class,       "ApiRequestEvent"),
                new NamedType(ApiResponseEvent.class,      "ApiResponseEvent"),
                new NamedType(BeneficiarySearchEvent.class,"BeneficiarySearchEvent"),
                new NamedType(ContractSearchEvent.class,   "ContractSearchEvent"),
                new NamedType(ErrorEvent.class,            "ErrorEvent"),
                new NamedType(FileEvent.class,             "FileEvent"),
                new NamedType(JobStatusChangeEvent.class,  "JobStatusChangeEvent"),
                new NamedType(JobSummaryEvent.class,       "JobSummaryEvent"),
                new NamedType(MetricsEvent.class,          "MetricsEvent"),
                new NamedType(ReloadEvent.class,           "ReloadEvent"),
                new NamedType(SlackEvents.class,           "SlackEvents")
        );
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.setMixInAnnotations(LoggableEvent.class, LoggableEventMixin.class);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    private static abstract class LoggableEventMixin { }
}
