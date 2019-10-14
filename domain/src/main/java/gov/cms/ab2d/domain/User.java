package gov.cms.ab2d.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "`user`")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String userID;
    private String name;
    private String email;
    private Boolean accountDisabled;

    @ManyToOne
    private Sponsor sponsor;

}
