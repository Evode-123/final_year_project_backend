package backend.tdms.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "packages")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", unique = true, nullable = false)
    private String trackingNumber;

    // Sender Information
    @Column(name = "sender_names", nullable = false)
    private String senderNames;

    @Column(name = "sender_phone", nullable = false)
    private String senderPhone;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "sender_id_number")
    private String senderIdNumber;

    @Column(name = "sender_address")
    private String senderAddress;

    // Receiver Information
    @Column(name = "receiver_names", nullable = false)
    private String receiverNames;

    @Column(name = "receiver_phone", nullable = false)
    private String receiverPhone;

    @Column(name = "receiver_email")
    private String receiverEmail;

    @Column(name = "receiver_id_number", nullable = false)
    private String receiverIdNumber;

    @Column(name = "receiver_address")
    private String receiverAddress;

    // Package Details
    @Column(name = "package_description", length = 500)
    private String packageDescription;

    @Column(name = "package_weight")
    private Double packageWeight;

    @Column(name = "package_value")
    private BigDecimal packageValue;

    @Column(name = "is_fragile")
    private Boolean isFragile = false;

    // Trip Information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_trip_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DailyTrip dailyTrip;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "PENDING";

    @Column(name = "payment_method")
    private String paymentMethod;

    // Status Tracking
    @Column(name = "package_status", nullable = false)
    private String packageStatus = "PENDING_PICKUP";

    @Column(name = "expected_arrival_time")
    private LocalDateTime expectedArrivalTime;

    @Column(name = "actual_arrival_time")
    private LocalDateTime actualArrivalTime;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "collected_by_name")
    private String collectedByName;

    @Column(name = "collected_by_id")
    private String collectedById;

    // System Information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by_user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User bookedBy;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    // Notifications
    @Column(name = "sender_notified_sent")
    private Boolean senderNotifiedSent = false;

    @Column(name = "receiver_notified_sent")
    private Boolean receiverNotifiedSent = false;

    @Column(name = "receiver_notified_arrived")
    private Boolean receiverNotifiedArrived = false;

    @Column(name = "sender_notified_delivered")
    private Boolean senderNotifiedDelivered = false;

    @PrePersist
    protected void onCreate() {
        bookingDate = LocalDateTime.now();
        if (trackingNumber == null) {
            trackingNumber = generateTrackingNumber();
        }
    }

    private String generateTrackingNumber() {
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%05d", (int)(Math.random() * 100000));
        return "PKG-" + date + "-" + random;
    }
}