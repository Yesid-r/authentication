package com.example.authentication.service.implementation;

import com.example.authentication.controller.DTO.requests.*;
import com.example.authentication.controller.DTO.responses.GeneralApiResponse;
import com.example.authentication.controller.DTO.responses.RegisterResponse;
import com.example.authentication.controller.DTO.responses.RegisterVerifyResponse;
import com.example.authentication.controller.DTO.responses.UserProfile;
import com.example.authentication.exceptions.ResourceNotFoundException;
import com.example.authentication.model.User;
import com.example.authentication.repository.UserRepository;
import com.example.authentication.service.AuthenticationService;
import com.example.authentication.service.EmailService;
import com.example.authentication.service.JwtService;
import com.example.authentication.service.OtpService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImplementation implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final CacheManager cacheManager;
    private final AuthenticationManager authenticationManager;




    @Override
    public ResponseEntity<RegisterResponse> registerUser(RegisterRequest registerRequest) {
        try {
            log.info("Received request to register user with email {}", registerRequest.getEmail());
            Optional<User> existingUserOpt = userRepository.findByEmail(registerRequest.getEmail().trim().toLowerCase());
            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();
                log.info("User already exists with email {}", registerRequest.getEmail());
                if (existingUser.getIsVerified()) {
                    return new ResponseEntity<>(RegisterResponse.builder()
                            .message("User already exists")
                            .build(), HttpStatus.BAD_REQUEST);
                } else {
                    log.info("User already exists but not verified with email {}, so their details will be updated", registerRequest.getEmail());
                    updateUserDetails(existingUser, registerRequest);
                    String otpToBeMailed = otpService.getOtpForEmail(registerRequest.getEmail());
                    CompletableFuture<Integer> emailResponse = emailService.sendEmailWithRetry(registerRequest.getEmail(), otpToBeMailed);
                    if (emailResponse.get() == -1) {
                        return new ResponseEntity<>(RegisterResponse.builder()
                                .message("Failed to send OTP email. Please try again later.")
                                .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    userRepository.save(existingUser);
                    return new ResponseEntity<>(RegisterResponse.builder()
                            .message("An email with OTP has been sent to your email address. Kindly verify.")
                            .build(), HttpStatus.CREATED);
                }
            } else {
                log.info("User does not exist with email {}, so this user will be created", registerRequest.getEmail());
                User newUser = createUser(registerRequest);
                String otpToBeMailed = otpService.getOtpForEmail(registerRequest.getEmail());
                CompletableFuture<Integer> emailResponse = emailService.sendEmailWithRetry(registerRequest.getEmail(),otpToBeMailed);
                if (emailResponse.get() == -1) {
                    return new ResponseEntity<>(RegisterResponse.builder()
                            .message("Failed to send OTP email. Please try again later.")
                            .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
                userRepository.save(newUser);
                log.info("User saved with the email {}", registerRequest.getEmail());
                return new ResponseEntity<>(RegisterResponse.builder()
                        .message("An email with OTP has been sent to your email address. Kindly verify.")
                        .build(), HttpStatus.CREATED);
            }
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send OTP email for user with email {}", registerRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RegisterResponse.builder()
                    .message("Failed to send OTP email. Please try again later.")
                    .build());
        }
        catch (Exception e) {
            log.error("Failed to register user with email {}", registerRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RegisterResponse.builder()
                    .message("Failed to register user. Please try again later.")
                    .build());
        }
    }

    @Override
    public ResponseEntity<?> verifyUserRegistration(RegisterVerifyRequest registerVerifyRequest) {
        String emailEntered = registerVerifyRequest.getEmail().trim().toLowerCase();
        String otpEntered = registerVerifyRequest.getOtp().trim();
        try {
            User user = userRepository.findByEmail(emailEntered).orElseThrow(
                    ResourceNotFoundException::new
            );
            String cachedOtp = cacheManager.getCache("user").get(emailEntered, String.class);
            if (cachedOtp == null) {
                log.info("the otp is not present in cache memory, it has expired for user {}, kindly retry and Register", emailEntered);
                return new ResponseEntity<>(GeneralApiResponse.builder().message("Otp has been expired for user " + emailEntered).build(), HttpStatus.REQUEST_TIMEOUT);
            } else if (!otpEntered.equals(cachedOtp)) {
                log.info("the entered otp does not match the otp Stored in cache for email {}", emailEntered);
                return new ResponseEntity<>(GeneralApiResponse.builder().message("Incorrect otp has been entered").build(), HttpStatus.BAD_REQUEST);
            } else {
                user.setIsVerified(true);
                userRepository.save(user);
                cacheManager.getCache("user").evict(emailEntered);
                log.info("the user email {} is successfully verified", user.isEnabled());
                RegisterVerifyResponse jwtToken = jwtService.generateJwtToken(user);
                return new ResponseEntity<>(jwtToken, HttpStatus.CREATED);

            }
        } catch (ResourceNotFoundException ex) {
            log.info("user with email {} not found in database", emailEntered);
            return new ResponseEntity<>(GeneralApiResponse.builder().message("user with this email does not exist").build(), HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<?> loginUser(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase();
        String password = loginRequest.getPassword();
        try {
            log.debug("Attempting to find user with email: {}", email);
            User user = userRepository.findByEmail(email).orElseThrow(
                    ResourceNotFoundException::new
            );
            log.debug("User found: {}", user.getEmail());
            log.debug("Attempting authentication for user: {}", email);
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            log.debug("Authentication successful for user: {}", email);
            if (!user.getIsVerified()) {

                return new ResponseEntity<>(GeneralApiResponse.builder().message("User is not verified").build(), HttpStatus.BAD_REQUEST);
            }

            RegisterVerifyResponse jwtToken = jwtService.generateJwtToken(user);
            return new ResponseEntity<>(jwtToken, HttpStatus.OK);

        } catch (ResourceNotFoundException ex) {
            log.info("user whose email is {} not found in Database", email);
            return new ResponseEntity<>(GeneralApiResponse.builder().message("User with this email does not exist").build(), HttpStatus.NOT_FOUND);
        }
        catch (Exception e) {
            log.error("Failed to authenticate user with email {}", email, e);
            return new ResponseEntity<>(GeneralApiResponse.builder().message("Invalid credentials").build(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<?> resendOtp(ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail().trim().toLowerCase();
        try {
            User user = userRepository.findByEmail(email).orElseThrow(
                    ResourceNotFoundException::new
            );
            if (cacheManager.getCache("user").get(email, String.class) != null) {
                log.info("the otp is already present in cache memory for user {}, kindly retry after some time", email);
                return new ResponseEntity<>(GeneralApiResponse.builder().message("Kindly retry after 1 minute").build(), HttpStatus.TOO_MANY_REQUESTS);
            }
            String otpToBeSend = otpService.getOtpForEmail(email);
            CompletableFuture<Integer> emailResponse= emailService.sendEmailWithRetry(email,otpToBeSend);
            if (emailResponse.get() == -1) {
                return new ResponseEntity<>(GeneralApiResponse.builder().message("Failed to send OTP email. Please try again later.").build(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return  new ResponseEntity<>(GeneralApiResponse.builder().message("An email with OTP has been sent to your email address. Kindly verify.").build(), HttpStatus.OK);

        } catch ( UnsupportedEncodingException e) {
            log.error("Failed to send OTP email for user with email {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RegisterResponse.builder()
                    .message("Failed to send OTP email. Please try again later.")
                    .build());
        } catch (ResourceNotFoundException ex) {
            log.info("user with email {} not found in Database", email);
            return new ResponseEntity<>(GeneralApiResponse.builder().message("User with email not found in database").build(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to resend OTP for user with email {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RegisterResponse.builder()
                    .message("Failed to resend OTP. Please try again later.")
                    .build());
        }
    }

    @Override
    public ResponseEntity<?> verifyOtp(RegisterVerifyRequest registerVerifyRequest) {
        String email = registerVerifyRequest.getEmail().trim().toLowerCase();
        if (registerVerifyRequest.getOtp() == null || registerVerifyRequest.getOtp().isEmpty()) {
            return new ResponseEntity<>(GeneralApiResponse.builder().message("Otp is empty").build(), HttpStatus.BAD_REQUEST);
        }
        String otp = registerVerifyRequest.getOtp().trim();
        try {
            User user = userRepository.findByEmail(email).orElseThrow(
                    ResourceNotFoundException::new
            );
        } catch (ResourceNotFoundException ex) {
            log.info("user with email {} not found in database ", email);
            return new ResponseEntity<>(GeneralApiResponse.builder().message("iUser with this email does not exist").build(), HttpStatus.NOT_FOUND);
        }

        String cachedOtp = cacheManager.getCache("user").get(email, String.class);
        if (cachedOtp == null) {
            log.info("the otp is not present in cache memory, it has expired for user {}, kindly retry", email);
            return new ResponseEntity<>(GeneralApiResponse.builder().message("Otp has been expired for user " + email).build(), HttpStatus.REQUEST_TIMEOUT);
        } else if (!otp.equals(cachedOtp)) {
            log.info("entered otp does not match the otp Stored in cache for email {}", email);
            return new ResponseEntity<>(GeneralApiResponse.builder().message("Incorrect otp has been entered").build(), HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(GeneralApiResponse.builder().message("otp verified successfully, now you can change the password").build(), HttpStatus.OK);
        }
    }

    @Override
    public ResponseEntity<?> resetPassword(ResetPasswordRequest resetPasswordRequest) {
        String email = resetPasswordRequest.getEmail().trim().toLowerCase();
        String newPassword = resetPasswordRequest.getPassword();
        String confirmPassword = resetPasswordRequest.getConfirmPassword();

        try {
            // 1. Verificar si el usuario existe y está verificado
            User user = userRepository.findByEmail(email).orElseThrow(
                    ResourceNotFoundException::new
            );

            if (!user.getIsVerified()) {
                log.info("Usuario con email {} no está verificado", email);
                return new ResponseEntity<>(GeneralApiResponse.builder()
                        .message("El usuario no está verificado")
                        .build(), HttpStatus.BAD_REQUEST);
            }

            // 2. Verificar si existe un OTP válido en memoria
            String cachedOtp = cacheManager.getCache("user").get(email, String.class);
            if (cachedOtp == null) {
                log.info("No hay un OTP válido en caché para el usuario {}, debe verificar OTP primero", email);
                return new ResponseEntity<>(GeneralApiResponse.builder()
                        .message("Debe verificar un OTP antes de cambiar la contraseña")
                        .build(), HttpStatus.BAD_REQUEST);
            }
            // implementar metodo verify otp
            if (!verifyOtp(new RegisterVerifyRequest(email, resetPasswordRequest.getOtp())).getStatusCode().is2xxSuccessful()) {
                log.info("No hay un OTP válido en caché para el usuario {}, debe verificar OTP primero", email);
                return new ResponseEntity<>(GeneralApiResponse.builder()
                        .message("Debe verificar un OTP antes de cambiar la contraseña")
                        .build(), HttpStatus.BAD_REQUEST);
            }


            // Verificar que las contraseñas coincidan
            if (!newPassword.equals(confirmPassword)) {
                return new ResponseEntity<>(GeneralApiResponse.builder()
                        .message("La contraseña y la confirmación de contraseña no coinciden")
                        .build(), HttpStatus.BAD_REQUEST);
            }

            // 3. Cambiar la contraseña
            user.setPassword(passwordEncoder.encode(newPassword));

            // 4. Actualizar el campo isUpdatePassword a true
            user.setIsUpdatePassword(true);

            // Guardar los cambios
            userRepository.save(user);

            // Limpiar el OTP de la caché después de cambiar la contraseña con éxito
            cacheManager.getCache("user").evict(email);

            return new ResponseEntity<>(GeneralApiResponse.builder()
                    .message("La contraseña ha sido restablecida con éxito")
                    .build(), HttpStatus.OK);
        } catch (ResourceNotFoundException ex) {
            log.info("Usuario con email {} no encontrado en la base de datos", email);
            return new ResponseEntity<>(GeneralApiResponse.builder()
                    .message("El usuario no existe con este correo electrónico")
                    .build(), HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<?> myProfile(ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail().trim().toLowerCase();
        try {
            User user = userRepository.findByEmail(email).orElseThrow(
                    ResourceNotFoundException::new
            );
            return new ResponseEntity<>(UserProfile.builder()
                    .id(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .gender(user.getGender())
                    .role(user.getRole())
                    .isOfficiallyEnabled(user.getIsVerified())
                    .build(), HttpStatus.OK);

        } catch (ResourceNotFoundException ex) {
            log.info("user with email {} not found in the Database", email);
            return new ResponseEntity<>(GeneralApiResponse.builder().message("user does not exist with this email").build(), HttpStatus.NOT_FOUND);
        }
    }

    private void updateUserDetails(User user, RegisterRequest registerRequest) {
        DUPLICATE_CODE(registerRequest, user);
    }

    private User createUser(RegisterRequest registerRequest) {
        User user = new User();
        DUPLICATE_CODE(registerRequest, user);
        return user;
    }

    private void DUPLICATE_CODE(RegisterRequest registerRequest, User user) {

        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(generateValidPassword()));
        user.setRole(registerRequest.getRole());
        user.setGender(registerRequest.getGender());
        user.setIsVerified(false);
        user.setIsActive(true);
        user.setIsUpdatePassword(false);
    }

    private String generateValidPassword() {
        int length = 12;

        Random random = new Random();
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";
        String specialChars = "@$!%*?&#";
        String allChars = lowercase + uppercase + digits + specialChars;

        StringBuilder password = new StringBuilder(length);

        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Fill remaining length with random characters
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Convert to char array for shuffling
        char[] passwordArray = password.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int j = random.nextInt(passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

}
