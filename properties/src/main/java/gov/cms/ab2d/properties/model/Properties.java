package gov.cms.ab2d.properties.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
@Table(name = "properties", schema = "property")
public class Properties extends TimestampBase {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="property.property_sequence")
    private Long id;
    @NotNull
    private String key;
    @NotNull
    private String value;
}
