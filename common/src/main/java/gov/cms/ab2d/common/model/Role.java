package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Role extends TimestampBase {

    @Id
    @GeneratedValue
    private Long id;

    @NaturalId
    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    private String name;

}
