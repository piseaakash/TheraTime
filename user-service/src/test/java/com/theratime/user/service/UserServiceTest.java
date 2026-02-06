package com.theratime.user.service;

import com.theratime.user.entity.UserEntity;
import com.theratime.user.exception.UserNotFoundException;
import com.theratime.user.model.User;
import com.theratime.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_setsDefaultTenantWhenMissing() {
        User user = new User();
        user.setEmail("a@b.com");

        UserEntity saved = new UserEntity();
        saved.setId(1L);
        saved.setEmail("a@b.com");
        saved.setTenantId(1L);

        when(userRepository.save(org.mockito.ArgumentMatchers.any(UserEntity.class))).thenReturn(saved);

        User result = userService.createUser(user);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTenantId()).isEqualTo(1L);
    }

    @Test
    void getUserById_notFound_returnsEmpty() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThat(userService.getUserById(99L)).isEmpty();
    }

    @Test
    void updateUser_notFound_throwsException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(1L, new User()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void createUser_withTenantId_preservesIt() {
        User user = new User();
        user.setEmail("a@b.com");
        user.setTenantId(5L);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity e = inv.getArgument(0);
            UserEntity ret = new UserEntity();
            ret.setId(1L);
            ret.setEmail(e.getEmail());
            ret.setTenantId(e.getTenantId());
            return ret;
        });

        User result = userService.createUser(user);

        assertThat(result.getTenantId()).isEqualTo(5L);
    }

    @Test
    void getUserByEmailId_found_returnsUser() {
        UserEntity entity = new UserEntity();
        entity.setId(2L);
        entity.setEmail("found@example.com");
        entity.setTenantId(1L);
        when(userRepository.findByEmail("found@example.com")).thenReturn(Optional.of(entity));

        assertThat(userService.getUserByEmailId("found@example.com")).isPresent();
        assertThat(userService.getUserByEmailId("found@example.com").get().getEmail()).isEqualTo("found@example.com");
        assertThat(userService.getUserByEmailId("found@example.com").get().getId()).isEqualTo(2L);
    }

    @Test
    void getUserByEmailId_notFound_returnsEmpty() {
        when(userRepository.findByEmail("none@example.com")).thenReturn(Optional.empty());
        assertThat(userService.getUserByEmailId("none@example.com")).isEmpty();
    }

    @Test
    void getUserById_found_returnsUser() {
        UserEntity entity = new UserEntity();
        entity.setId(3L);
        entity.setEmail("u@example.com");
        when(userRepository.findById(3L)).thenReturn(Optional.of(entity));

        assertThat(userService.getUserById(3L)).isPresent();
        assertThat(userService.getUserById(3L).get().getId()).isEqualTo(3L);
    }

    @Test
    void updateUser_withTenantId_setsIt() {
        UserEntity existing = new UserEntity();
        existing.setId(1L);
        existing.setTenantId(1L);
        User update = new User();
        update.setFirstName("F");
        update.setLastName("L");
        update.setTenantId(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(1L, update);

        assertThat(existing.getTenantId()).isEqualTo(10L);
    }

    @Test
    void updateUser_withoutTenantId_keepsExisting() {
        UserEntity existing = new UserEntity();
        existing.setId(1L);
        existing.setTenantId(7L);
        User update = new User();
        update.setFirstName("F");
        update.setLastName("L");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUser(1L, update);

        assertThat(existing.getTenantId()).isEqualTo(7L);
    }
}

