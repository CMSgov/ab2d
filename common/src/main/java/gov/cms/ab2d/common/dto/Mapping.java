package gov.cms.ab2d.common.dto;

import org.modelmapper.ModelMapper;

public class Mapping {

    private static final ModelMapper MODEL_MAPPER;

    static {
        MODEL_MAPPER = new ModelMapper();
    }

    public static ModelMapper getModelMapper() {
        return MODEL_MAPPER;
    }
}
