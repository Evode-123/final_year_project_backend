package backend.tdms.com.service;

import backend.tdms.com.dto.BookingWithPaymentDTO;
import backend.tdms.com.dto.AvailableTripDTO;
import backend.tdms.com.dto.SearchTripsDTO;
import backend.tdms.com.model.*;
import backend.tdms.com.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final DailyTripRepository dailyTripRepository;
    private final UserRepository userRepository;
    private final PaypackService paypackService; // ✅ Using Paypack
    private final PaymentTransactionRepository paymentTransactionRepository;

    /**
     * ✅ Create booking with Paypack payment
     * - OTHER_USER with MOBILE_MONEY: Requires Paypack payment (real phone prompt!)
     * - Staff or CASH: Immediately confirmed
     */
    @Transactional
    public Booking createBookingWithPayment(BookingWithPaymentDTO dto) {
        // Get current user
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is staff (bypass payment) or OTHER_USER (requires payment)
        boolean isStaff = currentUser.getRoles().equals("ROLE_ADMIN") 
            || currentUser.getRoles().equals("ROLE_MANAGER") 
            || currentUser.getRoles().equals("ROLE_RECEPTIONIST");

        // Use locking to prevent race conditions
        DailyTrip dailyTrip = dailyTripRepository
            .findByIdWithLock(dto.getDailyTripId())
            .orElseThrow(() -> new RuntimeException("Trip not found"));

        // Validate trip date
        LocalDate today = LocalDate.now();
        LocalDate maxBookingDate = today.plusDays(2);

        if (dailyTrip.getTripDate().isBefore(today)) {
            throw new RuntimeException("Cannot book trips in the past");
        }

        if (dailyTrip.getTripDate().isAfter(maxBookingDate)) {
            throw new RuntimeException("Can only book trips up to 2 days in advance");
        }

        // Check available seats
        if (dailyTrip.getAvailableSeats() <= 0) {
            throw new RuntimeException("No available seats for this trip");
        }

        // ✅ PAYPACK PAYMENT LOGIC
        PaymentTransaction paymentTransaction = null;
        String paymentStatus = "PENDING";
        String bookingStatus = "PENDING";

        if (!isStaff && dto.getRequiresPayment() && "MOBILE_MONEY".equals(dto.getPaymentMethod())) {
            // OTHER_USER with MOBILE_MONEY - Initiate Paypack payment
            try {
                log.info("Initiating Paypack payment for user: {}, phone: {}", 
                    currentUser.getEmail(), dto.getCustomerPhone());
                
                paymentTransaction = paypackService.initiateCashin(
                    dto.getCustomerPhone(),
                    dailyTrip.getRoute().getPrice()
                );
                
                // Booking stays PENDING until payment confirmed
                paymentStatus = "PENDING";
                bookingStatus = "PENDING";
                
                log.info("✅ Paypack payment initiated. Ref: {}", paymentTransaction.getPaypackRef());
                log.info("   Customer will receive prompt on phone: {}", dto.getCustomerPhone());
                
            } catch (Exception e) {
                log.error("Paypack payment initiation failed: {}", e.getMessage());
                throw new RuntimeException("Payment initiation failed. Please try again.");
            }
        } else {
            // Staff booking or CASH payment - Auto confirm
            paymentStatus = "PAID";
            bookingStatus = "CONFIRMED";
            log.info("Staff booking or CASH payment - auto-confirmed");
        }

        // Find or create customer
        Customer customer = customerRepository.findByPhoneNumber(dto.getCustomerPhone())
            .orElseGet(() -> {
                Customer newCustomer = new Customer();
                newCustomer.setNames(dto.getCustomerName());
                newCustomer.setPhoneNumber(dto.getCustomerPhone());
                return customerRepository.save(newCustomer);
            });

        // Create booking
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setDailyTrip(dailyTrip);
        booking.setBookedBy(currentUser);
        booking.setNumberOfSeats(1);
        booking.setPrice(dailyTrip.getRoute().getPrice());
        booking.setPaymentMethod(dto.getPaymentMethod());
        booking.setPaymentStatus(paymentStatus);
        booking.setBookingStatus(bookingStatus);

        // Store payment reference
        if (paymentTransaction != null) {
            booking.setPaypackRef(paymentTransaction.getPaypackRef());
        }

        // Assign seat number only if confirmed
        if ("CONFIRMED".equals(bookingStatus)) {
            Long confirmedBookings = bookingRepository
                .countConfirmedBookingsByTrip(dailyTrip.getId());
            booking.setSeatNumber(String.valueOf(confirmedBookings + 1));
            
            // Update available seats
            dailyTrip.setAvailableSeats(dailyTrip.getAvailableSeats() - 1);
            dailyTripRepository.save(dailyTrip);
        }

        // Save booking
        Booking savedBooking = bookingRepository.save(booking);

        // Link payment transaction to booking
        if (paymentTransaction != null) {
            paymentTransaction.setBooking(savedBooking);
            paymentTransactionRepository.save(paymentTransaction);
        }

        log.info("Booking created: Ticket={}, Status={}, Payment={}, PaypackRef={}", 
            savedBooking.getTicketNumber(), 
            bookingStatus,
            paymentStatus,
            savedBooking.getPaypackRef());

        return savedBooking;
    }

    /**
     * ✅ Confirm payment using Paypack
     */
    @Transactional
    public Booking confirmPayment(String paypackRef) {
        log.info("Confirming payment for ref: {}", paypackRef);
        
        // Find payment transaction
        PaymentTransaction transaction = paymentTransactionRepository
            .findByPaypackRef(paypackRef)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + paypackRef));

        // Check payment status from Paypack
        String status = paypackService.checkPaymentStatus(paypackRef);
        log.info("Payment status from Paypack: {}", status);

        if ("successful".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
            // Update transaction
            transaction.setStatus("SUCCESS");
            paymentTransactionRepository.save(transaction);

            // Update booking if exists
            if (transaction.getBooking() != null) {
                Booking booking = transaction.getBooking();
                
                // Lock the trip to update seats
                DailyTrip dailyTrip = dailyTripRepository
                    .findByIdWithLock(booking.getDailyTrip().getId())
                    .orElseThrow(() -> new RuntimeException("Trip not found"));

                // Update booking status
                booking.setPaymentStatus("PAID");
                booking.setBookingStatus("CONFIRMED");

                // Assign seat number
                Long confirmedBookings = bookingRepository
                    .countConfirmedBookingsByTrip(dailyTrip.getId());
                booking.setSeatNumber(String.valueOf(confirmedBookings + 1));

                // Update available seats
                dailyTrip.setAvailableSeats(dailyTrip.getAvailableSeats() - 1);
                dailyTripRepository.save(dailyTrip);

                Booking confirmedBooking = bookingRepository.save(booking);

                log.info("✅ Payment confirmed: Ticket={}, PaypackRef={}", 
                    confirmedBooking.getTicketNumber(), 
                    paypackRef);

                return confirmedBooking;
            }
        } else if ("failed".equalsIgnoreCase(status)) {
            transaction.setStatus("FAILED");
            paymentTransactionRepository.save(transaction);

            // Update booking to failed if exists
            if (transaction.getBooking() != null) {
                Booking booking = transaction.getBooking();
                booking.setPaymentStatus("FAILED");
                booking.setBookingStatus("CANCELLED");
                booking.setCancellationReason("Payment failed");
                bookingRepository.save(booking);
            }

            throw new RuntimeException("Payment failed");
        }

        throw new RuntimeException("Payment still pending");
    }

    // ============================================
    // Other methods remain exactly the same
    // ============================================
    
    public List<AvailableTripDTO> searchAvailableTrips(SearchTripsDTO searchDTO) {
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        List<DailyTrip> trips = dailyTripRepository.findAvailableTripsByRouteAndDateExcludingPast(
            searchDTO.getOrigin(),
            searchDTO.getDestination(),
            searchDTO.getTravelDate(),
            today,
            currentTime
        );

        return trips.stream()
            .map(this::convertToAvailableTripDTO)
            .collect(Collectors.toList());
    }

    public List<AvailableTripDTO> getAvailableTrips() {
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        LocalDate twoDaysLater = today.plusDays(2);

        List<DailyTrip> trips = dailyTripRepository.findAvailableTripsExcludingPast(
            today,
            currentTime,
            today,
            twoDaysLater
        );

        return trips.stream()
            .filter(trip -> trip.getAvailableSeats() > 0)
            .map(this::convertToAvailableTripDTO)
            .collect(Collectors.toList());
    }

    public Booking getBookingByTicketNumber(String ticketNumber) {
        return bookingRepository.findByTicketNumber(ticketNumber)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    public List<Booking> getCustomerBookings(String phoneNumber) {
        return bookingRepository.findActiveBookingsByPhone(phoneNumber);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (booking.getBookedBy() != null && 
            !booking.getBookedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only cancel your own bookings");
        }

        if (!"CONFIRMED".equals(booking.getBookingStatus())) {
            throw new RuntimeException("Only confirmed bookings can be cancelled");
        }

        if (booking.getDailyTrip().getTripDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot cancel bookings for past trips");
        }

        booking.setBookingStatus("CANCELLED");
        booking.setCancelledAt(java.time.LocalDateTime.now());
        booking.setCancellationReason(reason);
        booking.setCancelledBy(currentUserEmail);

        DailyTrip dailyTrip = booking.getDailyTrip();
        dailyTrip.setAvailableSeats(dailyTrip.getAvailableSeats() + 1);
        dailyTripRepository.save(dailyTrip);

        booking.setPaymentStatus("REFUNDED");

        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsForTrip(Long dailyTripId) {
        DailyTrip dailyTrip = dailyTripRepository.findById(dailyTripId)
            .orElseThrow(() -> new RuntimeException("Trip not found"));
        
        return bookingRepository.findByDailyTrip(dailyTrip);
    }

    public List<Booking> getTodayBookings() {
        java.time.LocalDateTime startOfDay = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        java.time.LocalDateTime endOfDay = java.time.LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        
        return bookingRepository.findBookingsBetweenDates(startOfDay, endOfDay);
    }

    public List<Booking> getMyActiveBookings() {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return bookingRepository.findAll().stream()
            .filter(booking -> 
                booking.getBookedBy() != null && 
                booking.getBookedBy().getId().equals(currentUser.getId()) &&
                "CONFIRMED".equals(booking.getBookingStatus())
            )
            .collect(Collectors.toList());
    }

    public List<Booking> getAllBookingsHistory() {
        return bookingRepository.findAll().stream()
            .sorted((b1, b2) -> b2.getBookingDate().compareTo(b1.getBookingDate()))
            .collect(Collectors.toList());
    }

    public List<Booking> getMyBookingHistory() {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return bookingRepository.findAll().stream()
            .filter(booking -> 
                booking.getBookedBy() != null && 
                booking.getBookedBy().getId().equals(currentUser.getId())
            )
            .sorted((b1, b2) -> b2.getBookingDate().compareTo(b1.getBookingDate()))
            .collect(Collectors.toList());
    }

    private AvailableTripDTO convertToAvailableTripDTO(DailyTrip trip) {
        AvailableTripDTO dto = new AvailableTripDTO();
        dto.setDailyTripId(trip.getId());
        dto.setOrigin(trip.getRoute().getOrigin());
        dto.setDestination(trip.getRoute().getDestination());
        dto.setTripDate(trip.getTripDate());
        dto.setDepartureTime(trip.getTimeSlot().getDepartureTime());
        dto.setVehiclePlateNo(trip.getVehicle().getPlateNo());
        dto.setAvailableSeats(trip.getAvailableSeats());
        dto.setTotalSeats(trip.getVehicle().getCapacity());
        dto.setPrice(trip.getRoute().getPrice().toString());
        return dto;
    }
}