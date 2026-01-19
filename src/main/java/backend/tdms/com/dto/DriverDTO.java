package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DriverDTO {
    private Long id;
    private String names;
    private String phoneNumber;
    private String licenseNo;
    private String idNumber;
    private LocalDate licenseExpiryDate;
    private LocalDate dateOfBirth;
    private String address;
    private Long assignedVehicleId;
    private String assignedVehiclePlateNo;
    private String status;
    private Boolean isBackup;
    private LocalDate hiredDate;
    private String email;
}