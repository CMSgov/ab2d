package gov.cms.ab2d.coverage.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
public class YearMonthRecord implements Serializable {
    private int year;
    private int month;
}
