package backend.tdms.com.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RouteDTO {
    private Long id;
    private String origin;
    private String destination;
    private BigDecimal price;
    private Integer durationMinutes;
    private Integer turnaroundBufferMinutes;
    private Boolean isActive;
}