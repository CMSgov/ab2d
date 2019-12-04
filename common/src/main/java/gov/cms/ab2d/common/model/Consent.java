package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
public class Consent {

    @Id
    @GeneratedValue
    private Long id;

    private String hicn;

    private OffsetDateTime effectiveDate;

    private String policyCode;

    private String purposeCode;

    private String loIncCode;

    private String scopeCode;
}
