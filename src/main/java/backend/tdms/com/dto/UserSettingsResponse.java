package backend.tdms.com.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean emailNotifications;
    private boolean smsNotifications;
}