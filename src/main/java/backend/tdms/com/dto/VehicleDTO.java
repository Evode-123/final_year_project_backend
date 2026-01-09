package backend.tdms.com.dto;

import lombok.Data;

@Data
public class VehicleDTO {
    private Long id;
    private String plateNo;
    private Integer capacity;
    private String vehicleType;
    private String status;
    private Boolean isActive;
}