package gov.cms.ab2d.common.properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import gov.cms.ab2d.common.model.Property;
import gov.cms.ab2d.common.repository.PropertiesRepository;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Optional;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class PropertiesServiceImplTest {

    @Mock
    PropertiesRepository propertiesRepository;

    @Captor
    ArgumentCaptor<Property> captor;

    @InjectMocks
    PropertiesServiceImpl propertiesService;

    @Test
    void getProperty() {
        when(propertiesRepository.findByKey("test")).thenReturn(Optional.of(property("test", "1234")));
        assertEquals("1234", propertiesService.getProperty("test", null));
    }

    @Test
    void getPropertyWithException(CapturedOutput out) {
        when(propertiesRepository.findByKey(any())).thenThrow(new RuntimeException("database error"));
        assertEquals("defaultValue", propertiesService.getProperty("test", "defaultValue"));
        assertTrue(out.getOut().contains("Error retrieving property 'test'"));
    }

    @Test
    void getNonExistentProperty(CapturedOutput out) {
        when(propertiesRepository.findByKey("test")).thenReturn(Optional.empty());
        assertEquals("defaultValue", propertiesService.getProperty("test", "defaultValue"));
        assertTrue(out.getOut().contains("Property 'test' not found; using default"));
    }

    @Test
    void updateProperty() {
        when(propertiesRepository.findByKey("test")).thenReturn(Optional.of(property("test", "1234")));
        assertTrue(propertiesService.updateProperty("test", "5678"));

        verify(propertiesRepository).saveAndFlush(captor.capture());
        assertEquals("5678", captor.getValue().getValue());
    }

    @Test
    void updatePropertyWithException(CapturedOutput out) {
        when(propertiesRepository.findByKey(any())).thenThrow(new RuntimeException("database error"));
        assertFalse(propertiesService.updateProperty("test", "5678"));
        assertTrue(out.getOut().contains("Error retrieving property 'test'"));
    }

    @Test
    void updateNonExistentProperty(CapturedOutput out) {
        when(propertiesRepository.findByKey("test")).thenReturn(Optional.empty());
        assertFalse(propertiesService.updateProperty("test", "5678"));
        assertTrue(out.getOut().contains("Unable to update 'test' - property not found"));
    }

    @Test
    void toggleOn() {
        when(propertiesRepository.findByKey("test")).thenReturn(Optional.of(property("test", "true")));
        assertTrue(propertiesService.isToggleOn("test", false));
    }

    @Test
    void toggleOff() {
        when(propertiesRepository.findByKey("test")).thenReturn(Optional.of(property("test", "false")));
        assertFalse(propertiesService.isToggleOn("test", true));
    }

    @Test
    void toggleUseDefaultValueIfEmpty() {
        assertFalse(propertiesService.isToggleOn("", false));
    }


    Property property(String key, String value) {
        val property = new Property();
        property.setKey(key);
        property.setValue(value);
        return property;
    }
}
