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

@Entity
@Table(name = "user_account")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class User extends TimestampBase implements UserDetails {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true)
    @NotNull
    private String username;
    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    @NotNull
    private Boolean enabled;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    @NotNull
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

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
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
