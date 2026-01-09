// Add this DTO class
package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponseDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean enabled;
    private boolean mustChangePassword;
    private boolean profileCompleted;
    private Set<String> roles;
    private LocalDateTime createdAt;
}