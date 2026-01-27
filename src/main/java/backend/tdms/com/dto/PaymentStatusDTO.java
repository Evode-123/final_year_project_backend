package backend.tdms.com.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusDTO {
    private String paypackRef;
    private String status;
    private String message;
    private Long bookingId;
    private String ticketNumber;
}