package backend.tdms.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bookings")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", unique = true, nullable = false)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_trip_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DailyTrip dailyTrip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by_user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User bookedBy;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @Column(name = "number_of_seats", nullable = false)
    private Integer numberOfSeats = 1;

    @Column(name = "seat_number")
    private String seatNumber;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "PENDING";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "booking_status", nullable = false)
    private String bookingStatus = "CONFIRMED";

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @PrePersist
    protected void onCreate() {
        bookingDate = LocalDateTime.now();
        if (ticketNumber == null) {
            ticketNumber = generateTicketNumber();
        }
    }

    private String generateTicketNumber() {
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%05d", (int)(Math.random() * 100000));
        return "TKT-" + date + "-" + random;
    }
}