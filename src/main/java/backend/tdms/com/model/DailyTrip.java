package backend.tdms.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "daily_trips")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DailyTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private TimeSlot timeSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Vehicle vehicle;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(nullable = false)
    private String status = "SCHEDULED";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (availableSeats == null && vehicle != null) {
            availableSeats = vehicle.getCapacity();
        }
        if (currentLocation == null) {
            currentLocation = "ORIGIN";
        }
    }
}