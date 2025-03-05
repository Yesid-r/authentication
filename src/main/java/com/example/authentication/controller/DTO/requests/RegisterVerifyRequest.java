package com.example.authentication.controller.DTO.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterVerifyRequest {
    @NotBlank(message = "Email no puede estar vacio")
    @Email(message = "Ingrese un email valido")
    private String email;
    @NotBlank(message = "OTP no puede estar vacio")
    @Size(min = 6, max = 6, message = "Codigo OTP  es de 6 caracteres")
    private String otp;
}
