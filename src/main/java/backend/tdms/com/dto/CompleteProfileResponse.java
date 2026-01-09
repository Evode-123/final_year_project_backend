package backend.tdms.com.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompleteProfileResponse {
    private String message;
    private String firstName;
    private String lastName;
    private String email;
    private boolean profileCompleted;
}