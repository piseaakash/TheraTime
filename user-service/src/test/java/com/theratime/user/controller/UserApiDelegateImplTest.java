package com.theratime.user.controller;

import com.theratime.user.model.User;
import com.theratime.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApiDelegateImplTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserApiDelegateImpl delegate;

    @Test
    void createUser_returns201AndBody() {
        User input = new User();
        input.setEmail("a@b.com");
        User saved = new User();
        saved.setId(1L);
        saved.setEmail("a@b.com");
        when(userService.createUser(any(User.class))).thenReturn(saved);

        ResponseEntity<User> result = delegate.createUser(input);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(saved);
        verify(userService).createUser(input);
    }

    @Test
    void getUserByEmail_found_returns200() {
        User user = new User();
        user.setId(1L);
        user.setEmail("a@b.com");
        when(userService.getUserByEmailId("a@b.com")).thenReturn(Optional.of(user));

        ResponseEntity<User> result = delegate.getUserByEmail("a@b.com");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(user);
    }

    @Test
    void getUserByEmail_notFound_returns404() {
        when(userService.getUserByEmailId("nobody@example.com")).thenReturn(Optional.empty());

        ResponseEntity<User> result = delegate.getUserByEmail("nobody@example.com");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isNull();
    }

    @Test
    void getUserById_found_returns200() {
        User user = new User();
        user.setId(1L);
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<User> result = delegate.getUserById(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(user);
    }

    @Test
    void getUserById_notFound_returns404() {
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        ResponseEntity<User> result = delegate.getUserById(99L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isNull();
    }

    @Test
    void updateUser_returns200AndBody() {
        User input = new User();
        input.setFirstName("New");
        User updated = new User();
        updated.setId(1L);
        updated.setFirstName("New");
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updated);

        ResponseEntity<User> result = delegate.updateUser(1L, input);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(updated);
        verify(userService).updateUser(1L, input);
    }
}
