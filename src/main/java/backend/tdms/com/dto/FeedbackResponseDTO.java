package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FeedbackResponseDTO {
    private Long id;
    private String customerName;
    private String customerEmail;
    private Integer rating;
    private String feedbackCategory;
    private String feedbackText;
    private String bookingReference;
    private Boolean isAnonymous;
    private String sentiment;
    private String status;
    private String adminResponse;
    private String respondedByEmail;
    private LocalDateTime respondedAt;
    private Boolean isFeatured;
    private Boolean readByUser; // NEW FIELD
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}