package gov.cms.ab2d.common.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertiesDTO {

    @NotNull
    private String key;

    @NotNull
    private String value;
}
