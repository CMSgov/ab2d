package gov.cms.ab2d.coverage.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@EqualsAndHashCode
public class YearMonthRecord implements Serializable {
    private int year;
    private int month;
}
