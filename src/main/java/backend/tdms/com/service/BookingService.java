package backend.tdms.com.service;

import backend.tdms.com.dto.BookingRequestDTO;
import backend.tdms.com.dto.AvailableTripDTO;
import backend.tdms.com.dto.SearchTripsDTO;
import backend.tdms.com.model.Booking;
import backend.tdms.com.model.Customer;
import backend.tdms.com.model.DailyTrip;
import backend.tdms.com.model.User;
import backend.tdms.com.repository.BookingRepository;
import backend.tdms.com.repository.CustomerRepository;
import backend.tdms.com.repository.DailyTripRepository;
import backend.tdms.com.repository.UserRepository;
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

    /**
     * Search available trips based on origin, destination, and date
     * ✅ UPDATED: Excludes past trips
     */
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

    /**
     * Get all available trips for the next 2 days
     * ✅ UPDATED: Excludes past trips
     */
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

    /**
     * Create booking with pessimistic locking to prevent overbooking
     */
    @Transactional
    public Booking createBooking(BookingRequestDTO dto) {
        // Use locking to prevent race conditions
        DailyTrip dailyTrip = dailyTripRepository
            .findByIdWithLock(dto.getDailyTripId())
            .orElseThrow(() -> new RuntimeException("Trip not found"));

        // Check if trip is in the future and within booking window
        LocalDate today = LocalDate.now();
        LocalDate maxBookingDate = today.plusDays(2);

        if (dailyTrip.getTripDate().isBefore(today)) {
            throw new RuntimeException("Cannot book trips in the past");
        }

        if (dailyTrip.getTripDate().isAfter(maxBookingDate)) {
            throw new RuntimeException("Can only book trips up to 2 days in advance");
        }

        // ✅ CRITICAL: Check available seats AFTER acquiring lock
        if (dailyTrip.getAvailableSeats() <= 0) {
            throw new RuntimeException("No available seats for this trip");
        }

        // Find or create customer
        Customer customer = customerRepository.findByPhoneNumber(dto.getCustomerPhone())
            .orElseGet(() -> {
                Customer newCustomer = new Customer();
                newCustomer.setNames(dto.getCustomerName());
                newCustomer.setPhoneNumber(dto.getCustomerPhone());
                return customerRepository.save(newCustomer);
            });

        // Get current logged-in user
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        User bookedByUser = userRepository.findByEmail(currentUserEmail)
            .orElse(null);

        // Create booking
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setDailyTrip(dailyTrip);
        booking.setBookedBy(bookedByUser);
        booking.setNumberOfSeats(1);
        booking.setPrice(dailyTrip.getRoute().getPrice());
        booking.setPaymentMethod(dto.getPaymentMethod());
        booking.setPaymentStatus("PAID");
        booking.setBookingStatus("CONFIRMED");

        // Assign seat number
        Long confirmedBookings = bookingRepository
            .countConfirmedBookingsByTrip(dailyTrip.getId());
        booking.setSeatNumber(String.valueOf(confirmedBookings + 1));

        // Update seats while still holding lock
        dailyTrip.setAvailableSeats(dailyTrip.getAvailableSeats() - 1);
        dailyTripRepository.save(dailyTrip);

        // Save booking
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking created with lock: Ticket {} for customer {} on trip {}", 
            savedBooking.getTicketNumber(), 
            customer.getNames(), 
            dailyTrip.getId());

        return savedBooking;
        // Lock automatically released when transaction commits
    }

    /**
     * Get booking by ticket number
     */
    public Booking getBookingByTicketNumber(String ticketNumber) {
        return bookingRepository.findByTicketNumber(ticketNumber)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    /**
     * Get customer bookings by phone number
     */
    public List<Booking> getCustomerBookings(String phoneNumber) {
        return bookingRepository.findActiveBookingsByPhone(phoneNumber);
    }

    /**
     * Cancel a booking
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"CONFIRMED".equals(booking.getBookingStatus())) {
            throw new RuntimeException("Only confirmed bookings can be cancelled");
        }

        // Check if trip is in the future
        if (booking.getDailyTrip().getTripDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot cancel bookings for past trips");
        }

        // Update booking status
        booking.setBookingStatus("CANCELLED");
        booking.setCancelledAt(java.time.LocalDateTime.now());
        booking.setCancellationReason(reason);
        
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        booking.setCancelledBy(currentUserEmail);

        // Restore available seat
        DailyTrip dailyTrip = booking.getDailyTrip();
        dailyTrip.setAvailableSeats(dailyTrip.getAvailableSeats() + 1);
        dailyTripRepository.save(dailyTrip);

        // Update payment status if refund needed
        booking.setPaymentStatus("REFUNDED");

        Booking cancelledBooking = bookingRepository.save(booking);

        log.info("Booking cancelled: Ticket {} by {}", 
            booking.getTicketNumber(), 
            currentUserEmail);

        return cancelledBooking;
    }

    /**
     * Get all bookings for a specific trip
     */
    public List<Booking> getBookingsForTrip(Long dailyTripId) {
        DailyTrip dailyTrip = dailyTripRepository.findById(dailyTripId)
            .orElseThrow(() -> new RuntimeException("Trip not found"));
        
        return bookingRepository.findByDailyTrip(dailyTrip);
    }

    /**
     * Get bookings for today
     */
    public List<Booking> getTodayBookings() {
        java.time.LocalDateTime startOfDay = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        java.time.LocalDateTime endOfDay = java.time.LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        
        return bookingRepository.findBookingsBetweenDates(startOfDay, endOfDay);
    }

    /**
     * Convert DailyTrip to AvailableTripDTO
     */
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