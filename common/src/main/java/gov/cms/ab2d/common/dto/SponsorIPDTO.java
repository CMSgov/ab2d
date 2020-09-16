package gov.cms.ab2d.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SponsorIPDTO {

    @NotNull
    private SponsorDTO sponsor;

    @NotNull
    private Set<String> ips = new HashSet<>();
}
