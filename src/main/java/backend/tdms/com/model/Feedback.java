package backend.tdms.com.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "feedback_category", nullable = false)
    private String feedbackCategory;

    @Column(name = "feedback_text", length = 1000)
    private String feedbackText;

    @Column(name = "booking_reference")
    private String bookingReference;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;

    @Column(nullable = false)
    private String sentiment = "NEUTRAL";

    @Column(name = "admin_response", length = 1000)
    private String adminResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by_id")
    private User respondedBy;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    // ✅ NEW FIELD: Track if user has read the admin response
    @Column(name = "read_by_user")
    private Boolean readByUser = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        if (rating != null) {
            if (rating >= 4) {
                sentiment = "POSITIVE";
            } else if (rating <= 2) {
                sentiment = "NEGATIVE";
            } else {
                sentiment = "NEUTRAL";
            }
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}