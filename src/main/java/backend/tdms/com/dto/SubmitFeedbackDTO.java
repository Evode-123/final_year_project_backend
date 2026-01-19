package backend.tdms.com.dto;

import lombok.Data;

@Data
public class SubmitFeedbackDTO {
    private Integer rating;
    private String feedbackCategory;
    private String feedbackText;
    private String bookingReference;
    private Boolean isAnonymous = false;
    
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}