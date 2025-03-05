package com.example.authentication.controller;

import com.example.authentication.controller.DTO.requests.*;
import com.example.authentication.controller.DTO.responses.GeneralApiResponse;
import com.example.authentication.controller.DTO.responses.RegisterResponse;
import com.example.authentication.controller.DTO.responses.UserProfile;
import com.example.authentication.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController{
    private final AuthenticationService authenticationService;


    @Operation(summary = "My Profile", description = "Retrieve user profile information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK: User profile retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserProfile.class))),
            @ApiResponse(responseCode = "404", description = "Not Found: User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralApiResponse.class))),

    })
    @PostMapping("/myProfile")
    public ResponseEntity<?> myProfile(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest){
        log.info("My profile request received for email {}", forgotPasswordRequest.getEmail());
        return authenticationService.myProfile(forgotPasswordRequest);
    }
}
