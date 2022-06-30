package gov.cms.ab2d.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PropertiesDTO {

    @NotNull
    private String key;

    @NotNull
    private String value;
}
