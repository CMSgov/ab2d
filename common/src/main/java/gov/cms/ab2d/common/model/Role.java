package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Role extends TimestampBase implements Serializable {

    public static final String SPONSOR_ROLE = "SPONSOR";
    public static final String ADMIN_ROLE = "ADMIN";
    public static final String ATTESTOR_ROLE = "ATTESTOR";

    @Id
    @GeneratedValue
    private Long id;

    @NaturalId
    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    private String name;

}
