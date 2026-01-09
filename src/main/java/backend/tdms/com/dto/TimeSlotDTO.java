package backend.tdms.com.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class TimeSlotDTO {
    private Long id;
    private LocalTime departureTime;
    private String slotName;
    private Boolean isActive;
}