package gov.cms.ab2d.bfd.client;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

/**
 * Because the location of the URL of the endpoint contains parsable content (previously
 * parsed by the @Value annotation, we have to do that manually to get the correct location
 * of the BFD URLs for each endpoint.
 */
@Component
public class UrlValueResolver implements EmbeddedValueResolverAware {
    private StringValueResolver embeddedValueResolver;

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    public String readMyProperty(String propertyString) {
        if (embeddedValueResolver == null) {
            return "";
        }
        return embeddedValueResolver.resolveStringValue(propertyString);
    }
}
