package backend.tdms.com.dto;

import lombok.Data;

@Data
public class IncidentStatisticsDTO {
    private Long totalIncidents;
    private Long reportedIncidents;
    private Long resolvedIncidents;
    private Long criticalIncidents;
    private Long delayIncidents;
    private Integer totalDelayMinutes;
    private Double averageDelayMinutes;
}