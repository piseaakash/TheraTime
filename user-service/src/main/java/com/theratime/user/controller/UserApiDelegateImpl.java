package com.theratime.user.controller;

import com.theratime.user.api.UserApiDelegate;
import com.theratime.user.entity.UserEntity;
import com.theratime.user.model.User;
import com.theratime.user.repository.UserRepository;
import com.theratime.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserApiDelegateImpl implements UserApiDelegate {

    private final UserService userService;

    @Override
    public ResponseEntity<User> createUser(User user) {
        User savedUser = userService.createUser(user);
        return ResponseEntity.status(201).body(savedUser);
    }

    @Override
    public ResponseEntity<User> getUserByEmail(String email) {
        Optional<User> user = userService.getUserByEmailId(email);
        //Optional<User> user = userEntity.map(this::mapToModel);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<User> getUserById(Long id) {
        //Optional<UserEntity> userEntity = userRepository.findById(id);
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<User> updateUser(Long id, User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }


}