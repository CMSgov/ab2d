package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class OptOut extends TimestampBase {

    @Id
    @GeneratedValue
    private Long id;
    private String hicn;
    private LocalDate effectiveDate;
    private String policyCode;
    private String purposeCode;
    private String loIncCode;
    private String scopeCode;
    private String mbi;
    private String ccwId;
    private String filename;
}
