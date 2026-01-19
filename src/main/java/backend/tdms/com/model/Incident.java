package backend.tdms.com.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_trip_id")
    private DailyTrip dailyTrip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_id", nullable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentType incidentType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentSeverity severity;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 200)
    private String location;

    @Column(nullable = false)
    private LocalDateTime incidentTime;

    @Column
    private Integer delayMinutes;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentStatus status = IncidentStatus.REPORTED;

    @Column(length = 1000)
    private String resolutionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    @Column
    private LocalDateTime resolvedAt;

    @Column
    private Boolean requiresMaintenance = false;

    @Column
    private Boolean affectsSchedule = false;

    @Column
    private Boolean passengersAffected = false;

    @Column
    private Integer affectedPassengerCount;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ✅ NEW FIELDS FOR FRONTEND TRACKING (replacing notification model)
    @Column(nullable = false)
    private LocalDateTime lastStatusChangeAt = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean hasUnviewedStatusUpdate = false;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum IncidentType {
        ACCIDENT,           // Traffic accident
        BREAKDOWN,          // Vehicle breakdown
        TRAFFIC_DELAY,      // Traffic jam
        WEATHER_DELAY,      // Weather-related delay
        MECHANICAL_ISSUE,   // Mechanical problem
        FLAT_TIRE,          // Tire puncture
        ROAD_CLOSURE,       // Road blocked
        FUEL_ISSUE,         // Fuel problem
        DRIVER_ISSUE,       // Driver unavailable/sick
        PASSENGER_INCIDENT, // Issue with passenger
        OTHER               // Other incidents
    }

    public enum IncidentSeverity {
        LOW,      // Minor, no significant impact
        MEDIUM,   // Moderate impact, manageable
        HIGH,     // Severe impact, immediate attention needed
        CRITICAL  // Emergency, urgent response required
    }

    public enum IncidentStatus {
        REPORTED,    // Just reported
        ACKNOWLEDGED, // Seen by management
        IN_PROGRESS, // Being handled
        RESOLVED,    // Fixed/completed
        CANCELLED    // False alarm or cancelled
    }
}