package gov.cms.ab2d.properties.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertiesDTO {

    @NotNull
    private String key;

    @NotNull
    private String value;
}
