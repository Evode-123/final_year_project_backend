package backend.tdms.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import backend.tdms.com.dto.UpdateEmailRequest;
import backend.tdms.com.dto.UpdateProfileRequest;
import backend.tdms.com.dto.UserSettingsResponse;
import backend.tdms.com.exception.AuthenticationFailedException;
import backend.tdms.com.model.Role;
import backend.tdms.com.model.User;
import backend.tdms.com.repository.RoleRepository;
import backend.tdms.com.repository.UserRepository;

@Service
public class SettingsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public UserSettingsResponse getUserSettings() {
        User user = getCurrentUser();
        return UserSettingsResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .emailNotifications(true)
                .smsNotifications(false)
                .build();
    }

    public void updateEmail(UpdateEmailRequest request) {
        User user = getCurrentUser();
        
        // Validate input
        if (request.getNewEmail() == null || request.getNewEmail().trim().isEmpty()) {
            throw new AuthenticationFailedException("Please enter a new email address.");
        }
        
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
            throw new AuthenticationFailedException("Please enter your current password.");
        }
        
        // Basic email format validation
        if (!request.getNewEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new AuthenticationFailedException("Please enter a valid email address.");
        }
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthenticationFailedException("Current password is incorrect. Please try again.");
        }
        
        // Check if new email is same as current
        if (user.getEmail().equalsIgnoreCase(request.getNewEmail().trim())) {
            throw new AuthenticationFailedException("New email is the same as your current email.");
        }
        
        // Check if new email already exists
        if (userRepository.findByEmail(request.getNewEmail()).isPresent()) {
            throw new AuthenticationFailedException("This email is already in use by another account.");
        }
        
        String oldEmail = user.getEmail();
        user.setEmail(request.getNewEmail().trim());
        userRepository.save(user);
        
        // Send notification emails
        boolean emailSent = emailService.sendEmailChangeNotification(
            oldEmail, 
            request.getNewEmail(), 
            user.getFirstName()
        );
        
        if (!emailSent) {
            System.out.println("Warning: Failed to send email change notification");
        }
    }

    public void updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        
        // Validate input
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new AuthenticationFailedException("First name cannot be empty.");
        }
        
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new AuthenticationFailedException("Last name cannot be empty.");
        }
        
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }
        
        userRepository.save(user);
    }

    public void deleteAccount(String password) {
        User user = getCurrentUser();
        
        // Validate password
        if (password == null || password.isEmpty()) {
            throw new AuthenticationFailedException("Please enter your password to confirm account deletion.");
        }
        
        // Verify password before deletion
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationFailedException("Password is incorrect. Please try again.");
        }
        
        // Check if user is an admin - prevent deletion of the last admin
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        
        if (isAdmin) {
            // Get admin role
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            
            // Count how many admins exist
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().contains(adminRole))
                    .count();
            
            // Prevent deletion if this is the last admin
            if (adminCount <= 1) {
                throw new AuthenticationFailedException(
                    "Cannot delete account. You are the only administrator in the system. " +
                    "Please create another admin account before deleting yours."
                );
            }
        } 
        
        // Send goodbye email
        boolean emailSent = emailService.sendAccountDeletionEmail(user.getEmail(), user.getFirstName());
        if (!emailSent) {
            System.out.println("Warning: Failed to send account deletion email");
        }
        
        // Delete user account
        userRepository.delete(user);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("User session expired. Please login again."));
    }
}