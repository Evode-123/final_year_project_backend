package backend.tdms.com.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompleteProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
}