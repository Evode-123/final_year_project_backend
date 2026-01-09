package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DriverCheckDTO {
    // Who & When
    private Long vehicleId;
    private Long driverId;
    private LocalDate checkDate;
    private String checkLocation; // WASH_GARAGE, BEFORE_TRIP, DEPOT
    
    // Quick Checks (true = OK, false = Problem, null = Not Checked)
    private Boolean tiresOk;
    private Boolean lightsOk;
    private Boolean brakesOk;
    private Boolean mirrorsOk;
    private Boolean windshieldOk;
    private Boolean wipersOk;
    private Boolean bodyDamage; // true = has damage
    private Boolean cleanlinessOk;
    
    // Safety Equipment
    private Boolean fireExtinguisher;
    private Boolean firstAidKit;
    private Boolean warningTriangle;
    
    // Fluids
    private Boolean oilLevelOk;
    private Boolean coolantLevelOk;
    private String fuelLevel; // FULL, HALF, LOW, EMPTY
    private Integer currentMileage;
    
    // Problems
    private Boolean hasProblems;
    private String problemsDescription;
    private Boolean isSafeToDrive;
    private String urgencyLevel; // LOW, MEDIUM, HIGH, CRITICAL
    
    // Driver Notes
    private String driverNotes;
}