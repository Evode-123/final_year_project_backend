package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VehicleInspectionDTO {
    private Long id;
    private Long vehicleId;
    private String vehiclePlateNo;
    private LocalDate inspectionDate;
    private LocalDate nextInspectionDue;
    private String inspectionStatus; // PASSED, FAILED, PENDING
    private String certificateNumber;
    private String notes;
    private String recordedByEmail;
    private Integer daysUntilDue;
    private String urgency; // OK, DUE_SOON, OVERDUE, NEVER_INSPECTED
}