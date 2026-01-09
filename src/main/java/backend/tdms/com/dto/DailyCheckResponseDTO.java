package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DailyCheckResponseDTO {
    private Long id;
    private Long vehicleId;
    private String vehiclePlateNo;
    private String vehicleType;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private LocalDate checkDate;
    private String checkLocation;
    private String overallStatus; // GOOD, HAS_ISSUES, URGENT
    
    // Checks Summary
    private Boolean tiresOk;
    private Boolean lightsOk;
    private Boolean brakesOk;
    private Boolean mirrorsOk;
    private Boolean windshieldOk;
    private Boolean wipersOk;
    private Boolean bodyDamage;
    private Boolean cleanlinessOk;
    private Boolean fireExtinguisher;
    private Boolean firstAidKit;
    private Boolean warningTriangle;
    private Boolean oilLevelOk;
    private Boolean coolantLevelOk;
    private String fuelLevel;
    private Integer currentMileage;
    
    // Problems
    private Boolean hasProblems;
    private String problemsDescription;
    private Boolean isSafeToDrive;
    private String urgencyLevel;
    private Boolean actionRequired;
    private String actionTaken;
    private Boolean followUpNeeded;
    
    // Review Status
    private Boolean reviewed;
    private String reviewedByEmail;
    private LocalDateTime reviewedAt;
    private String managerNotes;
    
    private LocalDateTime createdAt;
    private String driverNotes;
}