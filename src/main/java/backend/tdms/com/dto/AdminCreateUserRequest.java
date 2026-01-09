package backend.tdms.com.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCreateUserRequest {
    private String email;
    private String role;
}