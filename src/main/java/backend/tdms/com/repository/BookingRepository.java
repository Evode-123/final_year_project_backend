package backend.tdms.com.repository;

import backend.tdms.com.model.Booking;
import backend.tdms.com.model.Customer;
import backend.tdms.com.model.DailyTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // ✅ CRITICAL FIX: Find by ticket number with all relationships
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.customer " +
           "JOIN FETCH b.dailyTrip dt " +
           "JOIN FETCH dt.route " +
           "JOIN FETCH dt.timeSlot " +
           "JOIN FETCH dt.vehicle " +
           "LEFT JOIN FETCH b.bookedBy " +
           "WHERE b.ticketNumber = :ticketNumber")
    Optional<Booking> findByTicketNumber(@Param("ticketNumber") String ticketNumber);
    
    // ✅ Find by customer with eager loading
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.customer c " +
           "JOIN FETCH b.dailyTrip dt " +
           "JOIN FETCH dt.route " +
           "JOIN FETCH dt.timeSlot " +
           "JOIN FETCH dt.vehicle " +
           "WHERE c = :customer")
    List<Booking> findByCustomer(@Param("customer") Customer customer);
    
    // ✅ Find by daily trip with eager loading
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.customer " +
           "JOIN FETCH b.dailyTrip dt " +
           "JOIN FETCH dt.route " +
           "JOIN FETCH dt.timeSlot " +
           "JOIN FETCH dt.vehicle " +
           "LEFT JOIN FETCH b.bookedBy " +
           "WHERE dt = :dailyTrip")
    List<Booking> findByDailyTrip(@Param("dailyTrip") DailyTrip dailyTrip);
    
    // ✅ Find confirmed bookings by trip with eager loading
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.customer " +
           "JOIN FETCH b.dailyTrip dt " +
           "JOIN FETCH dt.route " +
           "JOIN FETCH dt.timeSlot " +
           "JOIN FETCH dt.vehicle " +
           "WHERE dt.id = :dailyTripId " +
           "AND b.bookingStatus = 'CONFIRMED'")
    List<Booking> findConfirmedBookingsByTrip(@Param("dailyTripId") Long dailyTripId);
    
    // ✅ Count confirmed bookings (no need for JOIN FETCH here - just counting)
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.dailyTrip.id = :dailyTripId " +
           "AND b.bookingStatus = 'CONFIRMED'")
    Long countConfirmedBookingsByTrip(@Param("dailyTripId") Long dailyTripId);
    
    // ✅ CRITICAL FIX: Find bookings between dates with all relationships
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.customer " +
           "JOIN FETCH b.dailyTrip dt " +
           "JOIN FETCH dt.route " +
           "JOIN FETCH dt.timeSlot " +
           "JOIN FETCH dt.vehicle " +
           "LEFT JOIN FETCH b.bookedBy " +
           "WHERE b.bookingDate >= :startDate " +
           "AND b.bookingDate <= :endDate " +
           "ORDER BY b.bookingDate DESC")
    List<Booking> findBookingsBetweenDates(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
    
    // ✅ CRITICAL FIX: Find active bookings by phone with all relationships
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.customer c " +
           "JOIN FETCH b.dailyTrip dt " +
           "JOIN FETCH dt.route " +
           "JOIN FETCH dt.timeSlot " +
           "JOIN FETCH dt.vehicle " +
           "LEFT JOIN FETCH b.bookedBy " +
           "WHERE c.phoneNumber = :phoneNumber " +
           "AND b.bookingStatus IN ('CONFIRMED', 'PAID') " +
           "ORDER BY b.bookingDate DESC")
    List<Booking> findActiveBookingsByPhone(@Param("phoneNumber") String phoneNumber);
}