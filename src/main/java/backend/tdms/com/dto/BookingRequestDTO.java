package backend.tdms.com.dto;

import lombok.Data;

@Data
public class BookingRequestDTO {
    private Long dailyTripId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String paymentMethod;
    private String notes;
}