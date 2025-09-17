package gov.cms.ab2d.contracts.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
