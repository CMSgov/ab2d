package gov.cms.ab2d.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Represents a PDP organization which possesses a single set of credentials for
 * accessing the system.
 */
@Entity
@Table(name = "user_account")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PdpClient extends TimestampBase implements UserDetails {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true)
    @NotNull
    private String clientId;

    @Column(unique = true)
    @NotNull
    private String organization;

    @NotNull
    private Boolean enabled;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_account_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();


    public void addRole(Role role) {
        roles.add(role);
    }

    @SuppressWarnings("unused")
    public void removeRole(Role role) {
        roles.remove(role);
    }

    @NotNull
    private Integer maxParallelJobs = 1;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : getRoles()) {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        }

        return authorities;
    }

    // Username is a required field for any UserDetails class
    // the username is the okta client id
    @Override
    public String getUsername() {
        return clientId;
    }

    @Override
    public String getPassword() {
        return null;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
