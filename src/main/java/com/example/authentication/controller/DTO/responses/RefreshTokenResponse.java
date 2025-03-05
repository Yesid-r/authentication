package com.example.authentication.controller.DTO.responses;

import com.example.authentication.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenResponse {
    private String accessToken;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
}
