package gov.cms.ab2d.worker.service.coveragesnapshot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoverageCountDTO {
    private String contractNumber;
    private String service;
    private int count;
    private int year;
    private int month;
    private Timestamp countedAt;
}
