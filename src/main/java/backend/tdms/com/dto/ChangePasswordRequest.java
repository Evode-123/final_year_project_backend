package backend.tdms.com.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {
    private String newPassword;
}