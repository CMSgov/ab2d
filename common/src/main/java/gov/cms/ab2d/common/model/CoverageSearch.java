package gov.cms.ab2d.common.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.OffsetDateTime;

@Entity
@Data
public class CoverageSearch {
    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    // The contract to search for mapping
    private String contract;

    // The month to search
    private Integer month;

    // The year to search (we're not always able to pass this to the search, but gives context)
    private Integer year;

    // We can use this to search for the earliest search request
    @CreationTimestamp
    private OffsetDateTime created;
}
