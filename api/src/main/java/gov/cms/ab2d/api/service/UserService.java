package gov.cms.ab2d.api.service;

import gov.cms.ab2d.api.repository.UserRepository;
import gov.cms.ab2d.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
/**
 * Just gets the current user from the authentication context.
 */
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth =  null; //SecurityContextHolder.getContext().getAuthentication(); leave out for now, otherwise it will not be null, just an anonymous user
        return auth != null ? userRepository.findByUserID(((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername()) : null;
    }

}
