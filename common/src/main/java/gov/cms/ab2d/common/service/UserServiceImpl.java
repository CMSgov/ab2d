package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.config.Mapping;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@Slf4j
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
        User user = userRepository.findByUsername(username);
        if (user == null) {
            String userNotPresentMsg = "User is not present in our database";
            log.error(userNotPresentMsg);
            throw new ResourceNotFoundException(userNotPresentMsg);
        } else {
            return user;
        }
    }

    @Override
    public UserDTO createUser(UserDTO userDTO) {
        User user = mapping.getModelMapper().map(userDTO, User.class);
        User createdUser = userRepository.saveAndFlush(user);
        return mapping.getModelMapper().map(createdUser, UserDTO.class);
    }

    @Override
    public UserDTO updateUser(UserDTO userDTO) {
        // When updating there needs to be verification that the user exists
        User user = getUserByUsername(userDTO.getUsername());
        userDTO.setId(user.getId());
        User mappedUser = mapping.getModelMapper().map(userDTO, User.class);
        User updatedUser = userRepository.saveAndFlush(mappedUser);
        return mapping.getModelMapper().map(updatedUser, UserDTO.class);
    }

    @Override
    public UserDTO enableUser(String username) {
        User user = getUserByUsername(username);
        user.setEnabled(true);

        User updatedUser = userRepository.saveAndFlush(user);
        return mapping.getModelMapper().map(updatedUser, UserDTO.class);
    }

    @Override
    public UserDTO disableUser(String username) {
        User user = getUserByUsername(username);
        user.setEnabled(false);

        User updatedUser = userRepository.saveAndFlush(user);
        return mapping.getModelMapper().map(updatedUser, UserDTO.class);
    }
}
