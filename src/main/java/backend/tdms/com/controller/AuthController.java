package backend.tdms.com.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
import backend.tdms.com.service.AuthenticationService;
//import backend.tdms.com.service.UserService;

//import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    //@Autowired
    //private UserService userService;

    /*@GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }
        User user = userService.getUserByEmail(principal.getName());
        // Map to DTO to match frontend expectation (only send role, or add more fields if needed)
        UserDTO dto = new UserDTO();
        dto.setRole(user.getRoles().iterator().next().getName());  // Assuming single role per user
        return ResponseEntity.ok(new ApiResponse<>(true, "User retrieved successfully", dto));
    }*/

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        AuthenticationResponse response = authenticationService.register(request);
        if (!response.isEmailSent()) {
            return ResponseEntity.ok()
                    .header("Warning", "User registered, but failed to send welcome email due to network issue.")
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/initial-change-password")
    public ResponseEntity<String> initialChangePassword(@RequestBody ChangePasswordRequest request) {
        authenticationService.initialChangePassword(request);
        return ResponseEntity.ok("Initial password changed successfully");
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<AuthenticationResponse> completeProfile(@RequestBody CompleteProfileRequest request) {
        AuthenticationResponse response = authenticationService.completeProfile(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        boolean emailSent = authenticationService.forgotPassword(request);
        if (!emailSent) {
            return ResponseEntity.ok("User found, but failed to send password reset email due to network issue. Please try again later.");
        }
        return ResponseEntity.ok("Password reset email sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }

    @PostMapping("/admin/create-user")
    public ResponseEntity<String> createUserByAdmin(@RequestBody AdminCreateUserRequest request) {
        boolean emailSent = authenticationService.createUserByAdmin(request);
        if (!emailSent) {
            return ResponseEntity.ok("User created successfully, but failed to send credentials email due to network issue.");
        }
        return ResponseEntity.ok("User created successfully");
    }

    @PostMapping("/admin/manage-user")
    public ResponseEntity<String> manageUser(@RequestBody ManageUserRequest request) {
        authenticationService.manageUser(request);
        return ResponseEntity.ok("User updated successfully");
    } 

    @GetMapping("/admin/users")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(authenticationService.getAllUsers());
    }
}

class UserDTO {
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}