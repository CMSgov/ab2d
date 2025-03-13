package gov.cms.ab2d.common.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

import java.io.Serializable;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Role extends TimestampBase implements Serializable {

    public static final String SPONSOR_ROLE = "SPONSOR";
    public static final String ADMIN_ROLE = "ADMIN";
    public static final String ATTESTOR_ROLE = "ATTESTOR";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq")
    @SequenceGenerator(name = "role_seq", sequenceName = "public.role_seq", allocationSize = 1)
    private Long id;

    @NaturalId
    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    private String name;

}
