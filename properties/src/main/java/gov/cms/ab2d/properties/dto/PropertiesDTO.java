package gov.cms.ab2d.properties.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class PropertiesDTO {
    public PropertiesDTO() {
        // Empty constructor still needed, even with all args constructor
    }

    @NotNull
    private String key;

    @NotNull
    private String value;
}
