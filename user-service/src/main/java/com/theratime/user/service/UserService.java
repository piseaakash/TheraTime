package com.theratime.user.service;


import com.theratime.user.entity.UserEntity;
import com.theratime.user.exception.UserNotFoundException;
import com.theratime.user.model.User;
import com.theratime.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user) {
        UserEntity userEntity = mapToEntity(user);
        if (userEntity.getTenantId() == null) {
            userEntity.setTenantId(DEFAULT_TENANT_ID);
        }
        UserEntity savedUserEntity = userRepository.save(userEntity);
        return mapToModel(savedUserEntity);
    }


    public Optional<User> getUserByEmailId(String email) {
        return userRepository.findByEmail(email)
                .map(this::mapToModel);
    }

    public Optional<User> getUserById(Long id) {
        Optional<UserEntity> userEntity = userRepository.findById(id);
        return userEntity.map(this::mapToModel);
    }

    public User updateUser(Long id, User user) {
        UserEntity existingUserEntity = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        existingUserEntity.setFirstName(user.getFirstName());
        existingUserEntity.setLastName(user.getLastName());
        existingUserEntity.setPhone(user.getPhone());
        if (user.getTenantId() != null) {
            existingUserEntity.setTenantId(user.getTenantId());
        }
        UserEntity updatedUserEntity = userRepository.save(existingUserEntity);
        return mapToModel(updatedUserEntity);
    }
    private static final long DEFAULT_TENANT_ID = 1L;

    private UserEntity mapToEntity(User user) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(user.getId());
        userEntity.setEmail(user.getEmail());
        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());
        userEntity.setPhone(user.getPhone());
        userEntity.setRole(user.getRole());
        userEntity.setTenantId(user.getTenantId() != null ? user.getTenantId() : DEFAULT_TENANT_ID);
        return userEntity;
    }

    private User mapToModel(UserEntity userEntity) {
        User user = new User();
        user.setId(userEntity.getId());
        user.setEmail(userEntity.getEmail());
        user.setFirstName(userEntity.getFirstName());
        user.setLastName(userEntity.getLastName());
        user.setPhone(userEntity.getPhone());
        user.setRole(userEntity.getRole());
        user.setTenantId(userEntity.getTenantId());
        return user;
    }
}
