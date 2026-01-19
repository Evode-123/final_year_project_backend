package backend.tdms.com.dto;

import backend.tdms.com.model.Incident.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IncidentResponseDTO {
    private Long id;
    private Long dailyTripId;
    private String tripRoute;
    private String tripDate;
    private String tripTime;
    private String vehiclePlateNo;
    private String driverName;
    private IncidentType incidentType;
    private IncidentSeverity severity;
    private String description;
    private String location;
    private LocalDateTime incidentTime;
    private Integer delayMinutes;
    private IncidentStatus status;
    private String reportedByName;
    private String reportedByEmail;
    private LocalDateTime reportedAt;
    private String resolutionNotes;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private Boolean requiresMaintenance;
    private Boolean affectsSchedule;
    private Boolean passengersAffected;
    private Integer affectedPassengerCount;
    
    // ✅ NEW: Frontend tracking fields
    private Boolean hasUnviewedStatusUpdate;
    private LocalDateTime lastStatusChangeAt;
}