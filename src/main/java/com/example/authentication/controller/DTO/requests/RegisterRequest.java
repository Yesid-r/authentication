package com.example.authentication.controller.DTO.requests;

import com.example.authentication.model.Gender;
import com.example.authentication.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank(message = "First name can't be blank")
    private String firstName;
    @NotBlank(message = "Last name can't be blank")
    private String lastName;
    @NotBlank(message = "Email can't be blank")
    @Email(message = "Invalid email entered")
    private String email;
    @NotNull(message ="Por favor seleccione genero")
    private Gender gender;
    @NotNull(message = "Porfavor seleccione un rol")
    private Role role;
}
