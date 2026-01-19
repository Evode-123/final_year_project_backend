package backend.tdms.com.dto;

import lombok.Data;

@Data
public class DailyTripResponseDTO {
    private Long dailyTripId;
    private String origin;
    private String destination;
    private String tripDate;
    private String departureTime;
    private String vehiclePlateNo;
    private String status;
    private Integer availableSeats;
    private String currentLocation;
}