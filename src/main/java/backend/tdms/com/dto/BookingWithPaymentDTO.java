package backend.tdms.com.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingWithPaymentDTO {
    private Long dailyTripId;
    private String customerName;
    private String customerPhone;
    private String paymentMethod; // CASH, MOBILE_MONEY, CARD
    private Boolean requiresPayment; // true for OTHER_USER, false for staff
}