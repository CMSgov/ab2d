package gov.cms.ab2d.common.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "properties", schema = "property")
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "propertiesSequenceGenerator")
    @SequenceGenerator(name = "propertiesSequenceGenerator",
            sequenceName = "property.property_sequence",
            allocationSize = 1)
    private Long id;

    @NotNull
    private String key;

    @NotNull
    private String value;

    @CreationTimestamp
    private OffsetDateTime created;

    @UpdateTimestamp
    private OffsetDateTime modified;
}
