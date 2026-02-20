package backend.tdms.com.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class PackageResponseDTO {
    private Long id;
    private String trackingNumber;
    
    // Sender
    private String senderNames;
    private String senderPhone;
    private String senderAddress;
    
    // Receiver
    private String receiverNames;
    private String receiverPhone;
    private String receiverIdNumber;
    private String receiverAddress;
    
    // Package Details
    private String packageDescription;
    private Double packageWeight;
    private BigDecimal packageValue;
    private Boolean isFragile;
    
    // Trip Details
    private String origin;
    private String destination;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private String vehiclePlateNo;
    
    // Status & Timing
    private String packageStatus;
    private BigDecimal price;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime bookingDate;
    private LocalDateTime expectedArrivalTime;
    private LocalDateTime actualArrivalTime;
    private LocalDateTime collectedAt;
    private String collectedByName;
}