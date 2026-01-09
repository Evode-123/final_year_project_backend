package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AvailableTripDTO {
    private Long dailyTripId;
    private String origin;
    private String destination;
    private LocalDate tripDate;
    private LocalTime departureTime;
    private String vehiclePlateNo;
    private Integer availableSeats;
    private Integer totalSeats;
    private String price;
}