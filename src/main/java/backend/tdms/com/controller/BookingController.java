package backend.tdms.com.controller;

import backend.tdms.com.dto.AvailableTripDTO;
import backend.tdms.com.dto.BookingRequestDTO;
import backend.tdms.com.dto.SearchTripsDTO;
import backend.tdms.com.model.Booking;
import backend.tdms.com.service.BookingService;
import backend.tdms.com.service.TicketPrintingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final TicketPrintingService ticketPrintingService;

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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER','RECEPTIONIST','OTHER_USER')")
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequestDTO bookingDTO) {
        Booking booking = bookingService.createBooking(bookingDTO);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

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

    // ✅ NEW: Get current user's active bookings
    @GetMapping("/my-bookings")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'OTHER_USER')")
    public ResponseEntity<List<Booking>> getMyActiveBookings() {
        List<Booking> bookings = bookingService.getMyActiveBookings();
        return ResponseEntity.ok(bookings);
    }

    // ✅ NEW: Get current user's booking history
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

    // ✅ UPDATED: Only for admin/manager/receptionist
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<Booking>> getTodayBookings() {
        List<Booking> bookings = bookingService.getTodayBookings();
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST','OTHER_USER', 'MANAGER')")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Booking booking = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(booking);
    }

    // TICKET PRINTING ENDPOINTS
    
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

    /**
     * ✅ NEW: Get all bookings history (for receptionist/admin/manager)
     * This allows staff to see all bookings for customer service purposes
     */
    @GetMapping("/all-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<Booking>> getAllBookingsHistory() {
        List<Booking> bookings = bookingService.getAllBookingsHistory();
        return ResponseEntity.ok(bookings);
    }

}