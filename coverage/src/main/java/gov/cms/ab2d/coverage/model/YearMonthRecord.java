package gov.cms.ab2d.coverage.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Embeddable
@Data
@AllArgsConstructor
public class YearMonthRecord {
    private int year;
    private int month;
}
