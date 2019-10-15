package gov.cms.ab2d.api.auth;

import gov.cms.ab2d.api.repository.UserRepository;
import gov.cms.ab2d.domain.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class AuthService implements UserDetailsService {


    @Autowired
    private UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repository.findByUserID(StringUtils.lowerCase(username));

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        if (user.getExpire().before(new Date())) {
            throw new UsernameNotFoundException("User account expired");
        }

        List<SimpleGrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("user"));
        return new org.springframework.security.core.userdetails.User(user.getUserID(), user.getPasswordHash(), authorities);
    }
}
