package gov.cms.ab2d.worker.config;

/**
 * The service that is responsible for gradual resizing of the patient processor thread pool
 * in response to load. See documentation for the implementing class for more details.
 */
public interface AutoScalingService {

    void autoscale();

    int getCorePoolSize();

    int getMaxPoolSize();

    double getScaleToMaxTime();
}
