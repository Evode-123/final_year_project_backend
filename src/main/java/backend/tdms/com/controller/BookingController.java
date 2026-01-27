package backend.tdms.com.controller;

import backend.tdms.com.dto.*;
import backend.tdms.com.model.Booking;
import backend.tdms.com.service.BookingService;
import backend.tdms.com.service.TicketPrintingService;
import backend.tdms.com.service.PaypackService; // ✅ Using Paypack
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final TicketPrintingService ticketPrintingService;
    private final PaypackService paypackService; // ✅ Using Paypack

    // ============================================
    // TRIP SEARCH ENDPOINTS
    // ============================================
    
    @PostMapping("/search")
    public ResponseEntity<List<AvailableTripDTO>> searchTrips(@RequestBody SearchTripsDTO searchDTO) {
        List<AvailableTripDTO> trips = bookingService.searchAvailableTrips(searchDTO);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/available")
    public ResponseEntity<List<AvailableTripDTO>> getAvailableTrips() {
        List<AvailableTripDTO> trips = bookingService.getAvailableTrips();
        return ResponseEntity.ok(trips);
    }

    // ============================================
    // BOOKING CREATION ENDPOINTS
    // ============================================
    
    /**
     * ✅ Create booking with Paypack payment integration
     * - OTHER_USER with MOBILE_MONEY: Requires Paypack payment (real phone prompt)
     * - Staff or CASH: Immediately confirmed
     */
    @PostMapping("/with-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER','RECEPTIONIST','OTHER_USER')")
    public ResponseEntity<Booking> createBookingWithPayment(@RequestBody BookingWithPaymentDTO bookingDTO) {
        Booking booking = bookingService.createBookingWithPayment(bookingDTO);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    /**
     * Legacy endpoint - kept for backward compatibility
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER','RECEPTIONIST','OTHER_USER')")
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequestDTO bookingDTO) {
        BookingWithPaymentDTO paymentDTO = BookingWithPaymentDTO.builder()
            .dailyTripId(bookingDTO.getDailyTripId())
            .customerName(bookingDTO.getCustomerName())
            .customerPhone(bookingDTO.getCustomerPhone())
            .paymentMethod(bookingDTO.getPaymentMethod())
            .requiresPayment(false)
            .build();
            
        Booking booking = bookingService.createBookingWithPayment(paymentDTO);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    // ============================================
    // PAYMENT ENDPOINTS
    // ============================================
    
    /**
     * ✅ Confirm payment after Paypack transaction
     */
    @PostMapping("/confirm-payment/{paypackRef}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER','RECEPTIONIST','OTHER_USER')")
    public ResponseEntity<Booking> confirmPayment(@PathVariable String paypackRef) {
        Booking booking = bookingService.confirmPayment(paypackRef);
        return ResponseEntity.ok(booking);
    }

    
    /**
     * ✅ Check payment status from Paypack
     * Handles "transaction not found" gracefully
     */
    @GetMapping("/payment-status/{paypackRef}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER','RECEPTIONIST','OTHER_USER')")
    public ResponseEntity<PaymentStatusDTO> checkPaymentStatus(@PathVariable String paypackRef) {
        try {
            log.info("Checking payment status for ref: {}", paypackRef);
            
            // Check payment status from Paypack
            String status = paypackService.checkPaymentStatus(paypackRef);
            
            log.info("Payment status result: {}", status);
            
            // ✅ Handle different status responses
            PaymentStatusDTO response;
            
            if ("ERROR".equals(status)) {
                // Transaction not found in Paypack yet (might be too soon)
                response = PaymentStatusDTO.builder()
                    .paypackRef(paypackRef)
                    .status("pending") // ✅ Treat ERROR as pending (transaction might be processing)
                    .message("Payment is being processed. Please wait...")
                    .build();
                    
                log.warn("Transaction not found in Paypack yet: {}. Treating as pending.", paypackRef);
                
            } else {
                // Valid status from Paypack
                response = PaymentStatusDTO.builder()
                    .paypackRef(paypackRef)
                    .status(status)
                    .message(getStatusMessage(status))
                    .build();
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking payment status for {}: {}", paypackRef, e.getMessage());
            
            // ✅ Return pending instead of error
            PaymentStatusDTO response = PaymentStatusDTO.builder()
                .paypackRef(paypackRef)
                .status("pending")
                .message("Payment is being processed. Please wait...")
                .build();
                
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Helper method to get user-friendly status messages
     */
    private String getStatusMessage(String status) {
        if (status == null) return "Checking payment status...";
        
        switch (status.toLowerCase()) {
            case "successful":
            case "success":
                return "Payment completed successfully";
            case "pending":
                return "Waiting for customer to complete payment on their phone";
            case "failed":
                return "Payment failed. Please try again";
            case "error":
                return "Payment is being processed. Please wait...";
            default:
                return "Payment status: " + status;
        }
    }

    // ============================================
    // BOOKING RETRIEVAL ENDPOINTS
    // ============================================
    
    @GetMapping("/ticket/{ticketNumber}")
    public ResponseEntity<Booking> getBookingByTicket(@PathVariable String ticketNumber) {
        Booking booking = bookingService.getBookingByTicketNumber(ticketNumber);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/customer/{phoneNumber}")
    public ResponseEntity<List<Booking>> getCustomerBookings(@PathVariable String phoneNumber) {
        List<Booking> bookings = bookingService.getCustomerBookings(phoneNumber);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'OTHER_USER')")
    public ResponseEntity<List<Booking>> getMyActiveBookings() {
        List<Booking> bookings = bookingService.getMyActiveBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'OTHER_USER')")
    public ResponseEntity<List<Booking>> getMyBookingHistory() {
        List<Booking> bookings = bookingService.getMyBookingHistory();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/trip/{dailyTripId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<Booking>> getBookingsForTrip(@PathVariable Long dailyTripId) {
        List<Booking> bookings = bookingService.getBookingsForTrip(dailyTripId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<Booking>> getTodayBookings() {
        List<Booking> bookings = bookingService.getTodayBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/all-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<Booking>> getAllBookingsHistory() {
        List<Booking> bookings = bookingService.getAllBookingsHistory();
        return ResponseEntity.ok(bookings);
    }

    // ============================================
    // BOOKING MANAGEMENT ENDPOINTS
    // ============================================
    
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST','OTHER_USER', 'MANAGER')")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Booking booking = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(booking);
    }

    // ============================================
    // TICKET PRINTING ENDPOINTS
    // ============================================
    
    @GetMapping("/ticket/{ticketNumber}/print")
    public ResponseEntity<String> printTicket(@PathVariable String ticketNumber) {
        Booking booking = bookingService.getBookingByTicketNumber(ticketNumber);
        String ticket = ticketPrintingService.generateTicketText(booking);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(ticket);
    }

    @GetMapping("/ticket/{ticketNumber}/print-html")
    public ResponseEntity<String> printTicketHTML(@PathVariable String ticketNumber) {
        Booking booking = bookingService.getBookingByTicketNumber(ticketNumber);
        String ticketHTML = ticketPrintingService.generateTicketHTML(booking);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(ticketHTML);
    }

    @GetMapping("/ticket/{ticketNumber}/receipt")
    public ResponseEntity<String> printReceipt(@PathVariable String ticketNumber) {
        Booking booking = bookingService.getBookingByTicketNumber(ticketNumber);
        String receipt = ticketPrintingService.generateReceipt(booking);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(receipt);
    }

    @GetMapping("/ticket/{ticketNumber}/download")
    public ResponseEntity<String> downloadTicket(@PathVariable String ticketNumber) {
        Booking booking = bookingService.getBookingByTicketNumber(ticketNumber);
        String ticketHTML = ticketPrintingService.generateTicketHTMLForDownload(booking);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"Ticket-" + ticketNumber + ".html\"")
                .contentType(MediaType.TEXT_HTML)
                .body(ticketHTML);
    }
}