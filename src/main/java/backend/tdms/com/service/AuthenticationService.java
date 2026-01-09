package backend.tdms.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import backend.tdms.com.dto.AdminCreateUserRequest;
import backend.tdms.com.dto.AuthenticationRequest;
import backend.tdms.com.dto.AuthenticationResponse;
import backend.tdms.com.dto.ChangePasswordRequest;
import backend.tdms.com.dto.CompleteProfileRequest;
import backend.tdms.com.dto.ForgotPasswordRequest;
import backend.tdms.com.dto.ManageUserRequest;
import backend.tdms.com.dto.RegisterRequest;
import backend.tdms.com.dto.ResetPasswordRequest;
import backend.tdms.com.dto.UserResponseDTO;
import backend.tdms.com.exception.AuthenticationFailedException;
import backend.tdms.com.model.Role;
import backend.tdms.com.model.User;
import backend.tdms.com.repository.RoleRepository;
import backend.tdms.com.repository.UserRepository;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmailService emailService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 12;
    private static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";

    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthenticationFailedException("An account with this email already exists. Please login or use a different email.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setMustChangePassword(false);
        user.setProfileCompleted(true);
        user.setEnabled(true);

        Role userRole = roleRepository.findByName("ROLE_OTHER_USER")
                .orElseThrow(() -> new RuntimeException("System configuration error. Please contact support."));
        user.setRoles(new HashSet<>(Collections.singleton(userRole)));

        userRepository.save(user);

        boolean emailSent = emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
        String jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .role("ROLE_OTHER_USER")
                .emailSent(emailSent)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mustChangePassword(false)
                .profileCompleted(true)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new AuthenticationFailedException("Please enter your email address.");
        }
        
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new AuthenticationFailedException("Please enter your password.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("No account found with this email address. Please check your email or sign up."));

        if (!user.isEnabled()) {
            throw new AuthenticationFailedException("Your account has been disabled. Please contact support for assistance.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            throw new AuthenticationFailedException("Incorrect password. Please try again or use 'Forgot Password' to reset it.");
        } catch (DisabledException e) {
            throw new AuthenticationFailedException("Your account has been disabled. Please contact support for assistance.");
        } catch (LockedException e) {
            throw new AuthenticationFailedException("Your account has been locked due to multiple failed login attempts. Please try again later or contact support.");
        } catch (AuthenticationException e) {
            throw new AuthenticationFailedException("Unable to sign in at this time. Please try again later.");
        }

        String jwtToken = jwtService.generateToken(user);
        String role = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("ROLE_OTHER_USER");

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .mustChangePassword(user.isMustChangePassword())
                .profileCompleted(user.isProfileCompleted())
                .role(role)
                .emailSent(true)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }

    /**
     * Creates an admin user ONLY if no admin exists in the system.
     * This method checks for the existence of any user with ROLE_ADMIN,
     * not just by the default email. This prevents duplicate admins when
     * the admin changes their email address.
     */
    public void createAdminOnStartup() {
        // Check if ANY user with admin role exists (using optimized query)
        boolean adminExists = userRepository.existsUserWithAdminRole();
        
        // Only create admin if no admin exists in the system
        if (!adminExists) {
            // Get the admin role
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            String randomPassword = generateRandomPassword();
            User admin = new User();
            admin.setEmail(DEFAULT_ADMIN_EMAIL);
            admin.setPassword(passwordEncoder.encode(randomPassword));
            admin.setMustChangePassword(true);
            admin.setProfileCompleted(false);
            admin.setEnabled(true);
            admin.setRoles(new HashSet<>(Collections.singleton(adminRole)));

            userRepository.save(admin);
            
            boolean emailSent = emailService.sendAdminCredentialsEmail(admin.getEmail(), randomPassword);
            if (!emailSent) {
                System.out.println("==========================================");
                System.out.println("WARNING: Failed to send admin credentials email");
                System.out.println("Admin Email: " + admin.getEmail());
                System.out.println("Admin Password: " + randomPassword);
                System.out.println("Please save these credentials securely!");
                System.out.println("==========================================");
            } else {
                System.out.println("==========================================");
                System.out.println("Admin account created successfully");
                System.out.println("Admin Email: " + admin.getEmail());
                System.out.println("Password sent to email");
                System.out.println("==========================================");
            }
        } else {
            System.out.println("Admin account already exists. Skipping admin creation.");
        }
    }

    public boolean createUserByAdmin(AdminCreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthenticationFailedException("A user with this email already exists.");
        }

        String randomPassword = generateRandomPassword();
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setMustChangePassword(true);
        user.setProfileCompleted(false);
        user.setEnabled(true);

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Invalid role specified."));
        user.setRoles(new HashSet<>(Collections.singleton(role)));

        userRepository.save(user);
        return emailService.sendUserCredentialsEmail(user.getEmail(), randomPassword, role.getName());
    }

    public void initialChangePassword(ChangePasswordRequest request) {
        User user = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new AuthenticationFailedException("User session expired. Please login again."));
        
        if (!user.isMustChangePassword()) {
            throw new AuthenticationFailedException("Password change is not required for your account.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new AuthenticationFailedException("Password must be at least 8 characters long.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new AuthenticationFailedException("User session expired. Please login again."));
        
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new AuthenticationFailedException("Password must be at least 8 characters long.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    public AuthenticationResponse completeProfile(CompleteProfileRequest request) {
        User user = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new AuthenticationFailedException("User session expired. Please login again."));
        
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new AuthenticationFailedException("First name is required.");
        }
        
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new AuthenticationFailedException("Last name is required.");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setProfileCompleted(true);
        userRepository.save(user);
        
        String role = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("ROLE_OTHER_USER");
        
        return AuthenticationResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(role)
                .profileCompleted(true)
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    public boolean forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("No account found with this email address."));
        
        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        userRepository.save(user);
        
        return emailService.sendPasswordResetEmail(
            user.getEmail(), 
            resetToken, 
            user.getFirstName()
        );
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new AuthenticationFailedException("Invalid or expired reset link. Please request a new password reset.");
        }

        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid or expired reset link. Please request a new password reset."));
        
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new AuthenticationFailedException("Password must be at least 8 characters long.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    public void manageUser(ManageUserRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("User not found."));
        
        if (request.getNewRole() != null && !request.getNewRole().isEmpty()) {
            Role role = roleRepository.findByName(request.getNewRole())
                    .orElseThrow(() -> new RuntimeException("Invalid role specified."));
            user.getRoles().clear();
            user.getRoles().add(role);
        }
        
        if (request.getIsEnabled() != null) {
            user.setEnabled(request.getIsEnabled());
        }
        
        userRepository.save(user);
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    private UserResponseDTO convertToDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setEnabled(user.isEnabled());
        dto.setMustChangePassword(user.isMustChangePassword());
        dto.setProfileCompleted(user.isProfileCompleted());
        dto.setRoles(user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet()));
        // Add createdAt if User entity has it
        return dto;
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}