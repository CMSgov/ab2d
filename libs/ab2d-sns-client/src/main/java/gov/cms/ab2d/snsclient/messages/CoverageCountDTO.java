package gov.cms.ab2d.snsclient.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;

@Getter
@Setter
@ToString
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
