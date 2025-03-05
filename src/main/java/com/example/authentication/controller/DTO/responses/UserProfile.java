package com.example.authentication.controller.DTO.responses;


import com.example.authentication.model.Gender;
import com.example.authentication.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Gender gender;
    private Role role;
    private Boolean isOfficiallyEnabled;
}
