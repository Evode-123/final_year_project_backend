package backend.tdms.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "daily_vehicle_checks")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DailyVehicleCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Driver driver;

    @Column(name = "check_date", nullable = false)
    private LocalDate checkDate;

    @Column(name = "check_location", nullable = false)
    private String checkLocation; // WASH_GARAGE, BEFORE_TRIP, DEPOT

    @Column(name = "overall_status", nullable = false)
    private String overallStatus; // GOOD, HAS_ISSUES, URGENT

    // Quick Visual Checks (Simple YES/NO or OK/PROBLEM)
    @Column(name = "tires_ok")
    private Boolean tiresOk;

    @Column(name = "lights_ok")
    private Boolean lightsOk;

    @Column(name = "brakes_ok")
    private Boolean brakesOk;

    @Column(name = "mirrors_ok")
    private Boolean mirrorsOk;

    @Column(name = "windshield_ok")
    private Boolean windshieldOk;

    @Column(name = "wipers_ok")
    private Boolean wipersOk;

    @Column(name = "body_damage")
    private Boolean bodyDamage; // true if there's new damage

    @Column(name = "cleanliness_ok")
    private Boolean cleanlinessOk;

    // Safety Equipment Check
    @Column(name = "fire_extinguisher")
    private Boolean fireExtinguisher;

    @Column(name = "first_aid_kit")
    private Boolean firstAidKit;

    @Column(name = "warning_triangle")
    private Boolean warningTriangle;

    // Fluid Levels (Simple check)
    @Column(name = "oil_level_ok")
    private Boolean oilLevelOk;

    @Column(name = "coolant_level_ok")
    private Boolean coolantLevelOk;

    @Column(name = "fuel_level")
    private String fuelLevel; // FULL, HALF, LOW, EMPTY

    @Column(name = "current_mileage")
    private Integer currentMileage;

    // Problems Reported
    @Column(name = "has_problems")
    private Boolean hasProblems = false;

    @Column(name = "problems_description", length = 2000)
    private String problemsDescription;

    @Column(name = "is_safe_to_drive")
    private Boolean isSafeToDrive = true;

    @Column(name = "urgency_level")
    private String urgencyLevel; // LOW, MEDIUM, HIGH, CRITICAL

    // Action Taken
    @Column(name = "action_required")
    private Boolean actionRequired = false;

    @Column(name = "action_taken", length = 500)
    private String actionTaken;

    @Column(name = "follow_up_needed")
    private Boolean followUpNeeded = false;

    // Manager Review
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "manager_notes", length = 500)
    private String managerNotes;

    // Photos (optional - for damage documentation)
    @Column(name = "photo_urls", length = 2000)
    private String photoUrls; // Comma-separated URLs if photos uploaded

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "driver_notes", length = 1000)
    private String driverNotes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        
        // Auto-determine overall status based on checks
        if (overallStatus == null) {
            overallStatus = determineOverallStatus();
        }
    }

    private String determineOverallStatus() {
        // If critical safety items are not OK, mark as URGENT
        if (Boolean.FALSE.equals(brakesOk) || Boolean.FALSE.equals(tiresOk) || 
            Boolean.FALSE.equals(isSafeToDrive)) {
            return "URGENT";
        }
        
        // If has problems but safe to drive
        if (Boolean.TRUE.equals(hasProblems)) {
            return "HAS_ISSUES";
        }
        
        // All good
        return "GOOD";
    }
}