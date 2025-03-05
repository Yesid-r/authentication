package com.example.authentication.controller.DTO.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @Email(message = "Ingrese un email valido")
    @NotBlank(message = "Email no puede estar vacio")
    private String email;
    @NotBlank(message = "Password no puede estar vacio")
    private String password;
}
