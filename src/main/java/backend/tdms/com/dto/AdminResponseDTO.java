package backend.tdms.com.dto;

import lombok.Data;

@Data
public class AdminResponseDTO {
    private Long feedbackId;
    private String adminResponse;
    private String status; // REVIEWED or RESOLVED
}