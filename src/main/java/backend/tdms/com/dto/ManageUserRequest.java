package backend.tdms.com.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManageUserRequest {
    private String email;
    private String newRole;
    private Boolean isEnabled;
}