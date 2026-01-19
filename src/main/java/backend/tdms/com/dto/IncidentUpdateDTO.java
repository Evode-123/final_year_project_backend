package backend.tdms.com.dto;

import backend.tdms.com.model.Incident.IncidentStatus;
import lombok.Data;

@Data
public class IncidentUpdateDTO {
    private IncidentStatus status;
    private String resolutionNotes;
}