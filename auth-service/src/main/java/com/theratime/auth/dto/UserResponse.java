package com.theratime.auth.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
