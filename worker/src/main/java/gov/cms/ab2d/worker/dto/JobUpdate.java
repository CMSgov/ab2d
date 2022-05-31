package gov.cms.ab2d.worker.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobUpdate {
    private String measure;
    private long value;
}
