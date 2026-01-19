package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalDateTime;

import backend.tdms.com.model.Incident.IncidentSeverity;
import backend.tdms.com.model.Incident.IncidentType;

@Data
public class IncidentReportDTO {
    private Long dailyTripId;
    private IncidentType incidentType;
    private IncidentSeverity severity;
    private String description;
    private String location;
    private LocalDateTime incidentTime;
    private Integer delayMinutes;
    private Boolean requiresMaintenance;
    private Boolean affectsSchedule;
    private Boolean passengersAffected;
    private Integer affectedPassengerCount;
}