package com.example.authentication.controller.DTO.responses;

import com.example.authentication.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVerifyResponse {
    private String accessToken ;
    private String refreshToken ;
    private String firstName ;
    private String lastName ;
    private String email ;
    private Role role;
    private boolean isVerified;
}
