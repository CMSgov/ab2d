package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Role extends TimestampBase {

    public static final String SPONSOR_ROLE = "SPONSOR";
    public static final String ADMIN_ROLE = "ADMIN";
    public static final String ATTESTOR_ROLE = "ATTESTOR";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NaturalId
    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    private String name;

}
