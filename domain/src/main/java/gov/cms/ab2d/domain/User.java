package gov.cms.ab2d.domain;

import javax.persistence.*;

@Entity
@Table(name = "`user`")
public class User {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String userID;
    private String name;
    private String email;
    private Boolean accountDisabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getAccountDisabled() {
        return accountDisabled;
    }

    public void setAccountDisabled(Boolean accountDisabled) {
        this.accountDisabled = accountDisabled;
    }
}
