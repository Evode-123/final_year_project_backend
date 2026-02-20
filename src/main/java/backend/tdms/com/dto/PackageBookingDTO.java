package backend.tdms.com.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PackageBookingDTO {
    
    // Sender Information
    private String senderNames;
    private String senderPhone;
    private String senderEmail;
    private String senderIdNumber;
    private String senderAddress;
    
    // Receiver Information
    private String receiverNames;
    private String receiverPhone;
    private String receiverEmail;
    private String receiverIdNumber;
    private String receiverAddress;
    
    // Package Details
    private String packageDescription;
    private Double packageWeight;
    private BigDecimal packageValue;
    private Boolean isFragile;
    
    // Trip Information
    private Long dailyTripId;
    
    // Payment
    private String paymentMethod; // CASH, MOBILE_MONEY, CARD
}