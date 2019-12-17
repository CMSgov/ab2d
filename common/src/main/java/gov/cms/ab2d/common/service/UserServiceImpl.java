package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.dto.Mapping;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
/**
 * Just gets the current user from the authentication context.
 */
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Mapping mapping;

    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? userRepository
                .findByUsername(
                        auth.getPrincipal() instanceof String ? (String) auth.getPrincipal() :
                                ((org.springframework.security.core.userdetails.User) auth
                                        .getPrincipal())
                                        .getUsername()) : null;
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User createUser(UserDTO userDTO) {
        User user = mapping.getModelMapper().map(userDTO, User.class);
        return userRepository.saveAndFlush(user);
    }
}
