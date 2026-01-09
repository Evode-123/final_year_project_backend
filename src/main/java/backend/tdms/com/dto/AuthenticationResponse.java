package backend.tdms.com.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private String token;
    private boolean mustChangePassword;
    private boolean profileCompleted;
    private String role;
    private boolean emailSent;
    
    // Add these new fields
    private String firstName;
    private String lastName;
    private String email;
}