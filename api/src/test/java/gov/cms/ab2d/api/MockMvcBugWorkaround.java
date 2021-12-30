package gov.cms.ab2d.api;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class MockMvcBugWorkaround {


    // Workaround to bug in tests introduced by Spring Boot 2.6.0
    // See GitHub issue https://github.com/spring-projects/spring-boot/issues/28759 and
    // https://github.com/spring-projects/spring-boot/issues/28818
    @Bean
    static BeanFactoryPostProcessor removeErrorSecurityFilter() {
        return (beanFactory) ->
                ((DefaultListableBeanFactory)beanFactory).removeBeanDefinition("errorPageSecurityInterceptor");
    }
}
