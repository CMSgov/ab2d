package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "`user`")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true)
    private String userID;
    private String name;
    private String email;
    private Boolean accountDisabled;

    @ManyToOne
    @JoinColumn(name = "sponsor_id")
    private Sponsor sponsor;
}
