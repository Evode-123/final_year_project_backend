package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SearchTripsDTO {
    private String origin;
    private String destination;
    private LocalDate travelDate;
}