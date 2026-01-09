package backend.tdms.com.dto;

import lombok.Data;

@Data
public class AssignDriverToVehicleDTO {
    private Long driverId;
    private Long vehicleId;
}