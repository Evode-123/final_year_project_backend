package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RecordInspectionDTO {
    private Long vehicleId;
    private LocalDate inspectionDate;
    private String inspectionStatus; // PASSED, FAILED
    private String certificateNumber;
    private String notes;
}