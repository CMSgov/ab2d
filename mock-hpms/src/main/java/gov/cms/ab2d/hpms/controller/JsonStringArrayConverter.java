package gov.cms.ab2d.hpms.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class JsonStringArrayConverter implements Converter<String, JsonStringArray> {

    private final ObjectMapper jsonMapper = new ObjectMapper();

    public JsonStringArray convert(@SuppressWarnings("NullableProblems") String source) {
        try {
            return new JsonStringArray(Arrays.asList(jsonMapper.readValue(source, String[].class)));
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
    }
}
