package com.example.authentication.controller.DTO.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ForgotPasswordRequest {

    @Email(message = "Ingrese un email valido")
    @NotBlank(message = "Email no puede ser vacio")
    private String email;
}
